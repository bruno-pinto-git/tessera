# Keycloak — Autenticacao e Autorizacao

## Visao Geral

O Keycloak e um servidor de identidade open-source que gere a autenticacao e autorizacao do sistema Tessera. Implementa os protocolos OpenID Connect (OIDC) e OAuth 2.0, fornecendo:

- Login e registo de utilizadores
- Gestao de sessoes e tokens JWT
- Controlo de acessos baseado em papeis (RBAC)
- Suporte a multiplos clientes (web, mobile, servicos)

Este documento cobre a **configuracao do Keycloak**. Para o **fluxo de
autenticacao** ponta-a-ponta no Tessera (JWT pass-through, validacao
nos servicos backend, etc.), ver [security.md](security.md).

## Versao e Execucao

- **Versao:** Keycloak 26.0
- **Modo:** Desenvolvimento (`start-dev`)
- **Porta:** 8180
- **Base de dados:** H2 embebida e **efemera** (sem volume persistente — o unico mount e o ficheiro de realm em read-only); em producao deve ser substituida por PostgreSQL
- **Importacao automatica:** O realm `tessera` e (re)importado a partir do ficheiro `infra/keycloak/realm-export.json` sempre que o container e recriado, ja que o estado nao e persistido

```yaml
keycloak:
  image: quay.io/keycloak/keycloak:26.0
  command: start-dev --import-realm
  environment:
    KC_HTTP_PORT: 8180
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  volumes:
    - ./infra/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro
```

## Realm: tessera

O realm `tessera` e o espaco de configuracao que contem todos os utilizadores, roles e clients da plataforma.

### Configuracoes do Realm

| Propriedade | Valor | Descricao |
|-------------|-------|-----------|
| Registo de utilizadores | Ativado | Novos adeptos podem criar conta |
| Login por email | Ativado | Permite login com endereco de email |
| Reset de password | Ativado | Utilizadores podem recuperar a password |
| Protecao brute force | Ativada | Bloqueia apos tentativas falhadas |
| SSL obrigatorio | Externo | Apenas em producao (desenvolvimento usa HTTP) |

## Roles (Papeis)

O realm define **quatro** roles de realm. Um utilizador pode acumular
roles; `club-manager` e `staff` sao scoped a clubes especificos atraves
de pertenca a grupos (`/clubs/<id>/managers` e `/clubs/<id>/staff`).

### platform-admin — Administrador da Plataforma

Administrador de sistema, transversal a todos os clubes. Permissoes:

- Visao de todos os clubes
- Gestao do catalogo (clubes, recintos)
- Criacao e edicao de utilizadores e atribuicao de gestores de clube
- Consulta de relatorios de bilheteira e estatisticas

### club-manager — Gestor de Clube

Gestor scoped a um ou mais clubes (via grupo `/clubs/<id>/managers`).
Permissoes:

- Gestao de jogadores e planteis do clube
- Criacao e gestao de jogos
- Configuracao de eventos de bilheteira (precos, estados)

### staff — Staff do Clube

Operacoes no dia de jogo, scoped a um ou mais clubes (via grupo
`/clubs/<id>/staff`). Permissoes:

- Validacao de bilhetes na entrada do recinto (leitura QR code, Android)
- Preenchimento da ficha tecnica do jogo em tempo real
- Registo de ocorrencias (golos, cartoes, substituicoes)
- Consulta de jogos e fichas tecnicas

### fan — Adepto

Utilizador final da plataforma. Permissoes:

- Compra de bilhetes digitais
- Consulta de jogos e resultados
- Visualizacao de fichas tecnicas e estatisticas
- Gestao do perfil pessoal

**Role por defeito:** O role por defeito do realm
(`default-roles-tessera`) e um role composto que inclui `fan`. Por isso,
quando um novo utilizador se regista (formulario de registo ou login
social Google), recebe efetivamente o role `fan`.

## Clients (Aplicacoes)

### tessera-web — React SPA

| Propriedade | Valor |
|-------------|-------|
| Tipo | Publico (sem secret) |
| Protocolo | OpenID Connect |
| Fluxo | Authorization Code + PKCE (S256); Direct Access Grants ativado |
| Redirect URIs | `http://localhost:5173/*`, `http://localhost:8000/*` |
| Web Origins | `http://localhost:5173`, `http://localhost:8000` |
| Token Lifespan | 1800s (30 minutos) |

Utilizado pela aplicacao web React. Como client publico, nao armazena credenciais no browser — utiliza PKCE (Proof Key for Code Exchange, metodo `S256`) para seguranca adicional no fluxo de autorizacao.

### tessera-android — Aplicacao Android

| Propriedade | Valor |
|-------------|-------|
| Tipo | Publico (sem secret) |
| Protocolo | OpenID Connect |
| Fluxo | Authorization Code + PKCE (S256) |
| Redirect URIs | `tessera://callback/*` |
| Token Lifespan | 3600s (60 minutos) |

Utilizado pela aplicacao movel Android. O redirect URI usa um custom scheme (`tessera://`) para interceptar o callback de autenticacao na app.

### tessera-bff — Servico BFF

| Propriedade | Valor |
|-------------|-------|
| Tipo | Confidencial (com secret) |
| Protocolo | OpenID Connect |
| Service Account | Ativado (`serviceAccountsEnabled = true`) |
| Secret | `change-me-in-production` |

Client confidencial usado para comunicacao servico-a-servico. A sua
conta de servico (utilizador `service-account-tessera-bff`) tem roles de
`realm-management` (`manage-users`, `view-users`, `manage-groups`, etc.)
e e usada pelo `KeycloakAdminClient` do **match-service** (grant
`client_credentials`) para chamar a **Keycloak Admin REST API** na
gestao de utilizadores e grupos (ver [security.md](security.md)).

## Identity Providers (Login Social)

O realm tem configurado login social atraves de identity providers
externos. O principal e o **Google**:

| Propriedade | Valor |
|-------------|-------|
| Alias | `google` |
| Provider ID | `google` |
| Estado | Ativado |
| Default Scope | `openid profile email` |

Esta tambem definido um **identity provider mapper** do tipo
`oidc-hardcoded-role-idp-mapper` (`google-fan-role`) que atribui
automaticamente o realm role `fan` aos utilizadores que entram via
Google, com `syncMode = FORCE` (reaplicado em cada login):

```json
{
  "name": "google-fan-role",
  "identityProviderAlias": "google",
  "identityProviderMapper": "oidc-hardcoded-role-idp-mapper",
  "config": { "role": "fan", "syncMode": "FORCE" }
}
```

Assim, qualquer utilizador que faca login com a sua conta Google fica
imediatamente como adepto (`fan`), sem atribuicao manual.

**Nota sobre utilizadores brokered:** apagar um utilizador criado via
Google **nao** e um banimento permanente — no proximo login Google o
Keycloak volta a aprovisiona-lo. Para bloquear efetivamente uma conta,
deve-se **desativar** (disable) em vez de apagar.

> Existe ainda um IdP `microsoft` definido no realm-export, mas sem
> mapper de role automatico associado.

## Gestao de Utilizadores via API

Para alem desta consola, a plataforma tem endpoints proprios de gestao
de utilizadores em `/api/v1/users` (match-service
`com.tessera.match.iam.UserController`, reencaminhados pelo BFF
`UserProxyController`). **Todos exigem o role `platform-admin`.**

Internamente, o match-service usa o `KeycloakAdminClient`, que se
autentica com a conta de servico do client confidencial `tessera-bff`
(grant `client_credentials`) e chama a **Keycloak Admin REST API** para
pesquisar, criar, atualizar e eliminar utilizadores, atribuir roles
(`club-manager`/`staff`) e gerir grupos de clube. A pagina
`/admin/users` do frontend assenta nestes endpoints. Detalhes em
[security.md](security.md).

## Utilizadores de Teste

Para desenvolvimento e testes, o realm inclui utilizadores pre-configurados:

| Username | Password | Email | Role |
|----------|----------|-------|------|
| `admin` | `admin` | admin@tessera.pt | platform-admin |
| `gestor` | `gestor` | gestor@tessera.pt | club-manager |
| `staff` | `staff` | staff@tessera.pt | staff |
| `adepto` | `adepto` | adepto@tessera.pt | fan |

Existe ainda a conta de servico `service-account-tessera-bff` (do client
confidencial `tessera-bff`), nao destinada a login interativo.

**Nota:** Estas credenciais sao apenas para desenvolvimento. Em producao, os utilizadores devem ser criados com passwords seguras.

## Protocol Mappers

Os clients `tessera-web` e `tessera-android` incluem tres protocol
mappers customizados:

| Mapper | Tipo | Claim | Descricao |
|--------|------|-------|-----------|
| `realm-roles` | `oidc-usermodel-realm-role-mapper` (multivalued) | `roles` | Realm roles do utilizador |
| `subject` | `oidc-usermodel-property-mapper` | `sub` | Id do utilizador |
| `groups` | `oidc-group-membership-mapper` (full path) | `groups` | Pertenca a grupos (path completo) |

O mapper `realm-roles` garante que os roles aparecem no claim `roles`,
permitindo ao frontend e aos servicos backend verificar permissoes sem
consultas adicionais ao Keycloak. O claim `groups` (com path completo)
e usado para resolver o scope de `club-manager`/`staff` por clube.

```json
{
  "name": "realm-roles",
  "protocolMapper": "oidc-usermodel-realm-role-mapper",
  "config": {
    "multivalued": "true",
    "claim.name": "roles",
    "access.token.claim": "true",
    "id.token.claim": "true",
    "userinfo.token.claim": "true"
  }
}
```

### Exemplo de Token JWT (payload)

```json
{
  "sub": "a1b2c3d4-...",
  "preferred_username": "gestor",
  "email": "gestor@tessera.pt",
  "realm_access": {
    "roles": ["club-manager", "default-roles-tessera"]
  },
  "roles": ["club-manager"],
  "groups": ["/clubs/1/managers"]
}
```

## Endpoints Principais

Todos os endpoints estao acessiveis via NGINX na porta 8000:

| Endpoint | Descricao |
|----------|-----------|
| `/realms/tessera/.well-known/openid-configuration` | Configuracao OIDC (discovery) |
| `/realms/tessera/protocol/openid-connect/token` | Obter/renovar tokens |
| `/realms/tessera/protocol/openid-connect/auth` | Iniciar fluxo de autorizacao |
| `/realms/tessera/protocol/openid-connect/userinfo` | Informacao do utilizador |
| `/admin/` | Consola de administracao |

## Consola de Administracao

Acessivel em `http://localhost:8180/admin/` ou `http://localhost:8000/admin/`.

Credenciais do realm master:
- **Username:** admin
- **Password:** admin

Na consola e possivel:
- Gerir utilizadores (criar, editar, atribuir roles)
- Configurar clients
- Visualizar sessoes ativas
- Consultar eventos de autenticacao

## Fluxo de Autenticacao (SPA)

```
1. Utilizador clica "Login" na SPA
2. SPA redireciona para /realms/tessera/protocol/openid-connect/auth
3. Keycloak apresenta formulario de login
4. Utilizador insere credenciais
5. Keycloak valida e redireciona para SPA com authorization code
6. SPA troca code por access token + refresh token
7. SPA inclui access token (Bearer) em todas as chamadas API
8. BFF valida o token com a chave publica do Keycloak
9. BFF encaminha o token ao servico downstream, que tambem o valida
```

## Integracao com os servicos backend

O **bff-service** e o **match-service** sao configurados como
OAuth2 Resource Servers via Spring Boot:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://keycloak:8180/realms/tessera/protocol/openid-connect/certs}
```

### Porque `jwk-set-uri` e nao `issuer-uri`?

O Keycloak emite tokens com o claim `iss = http://localhost:8180/realms/tessera`
(porto do host). Mas dentro da rede Docker, os servicos chegam ao
Keycloak em `http://keycloak:8180`. Se usassemos `issuer-uri`,
o Spring Security iria comparar o claim `iss` com este URL e o token
seria rejeitado por mismatch.

Usando `jwk-set-uri`, o Spring valida apenas a **assinatura
criptografica** do JWT contra a chave publica obtida do endpoint
JWKS — e ignora a comparacao do issuer.

Alternativas (mais complexas):

- Configurar `KC_HOSTNAME` no container Keycloak para emitir tokens
  com um issuer URL "estatico" (ex: `https://auth.tessera.example`)
- Ter dois Keycloak hostnames distintos consoante a rede (frontend
  vs backend)

A abordagem JWKS e a mais simples para desenvolvimento e suficiente
para producao desde que o JWKS endpoint seja acessivel.

## Importacao do Realm

O ficheiro `infra/keycloak/realm-export.json` e importado automaticamente sempre que o container e (re)criado, dado que o Keycloak corre com H2 efemera (sem volume persistente). Para re-importar (ex: apos alteracoes ao ficheiro):

1. Parar e remover o container Keycloak
2. Reiniciar — o Keycloak ira re-importar o ficheiro

```powershell
docker compose stop keycloak
docker compose rm -f keycloak
docker compose up -d keycloak
```

Ou utilizar o script `scripts/run/reset.ps1` que faz reset completo.
