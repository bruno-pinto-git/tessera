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
- **Base de dados:** H2 embebida (desenvolvimento); em producao deve ser substituida por PostgreSQL
- **Importacao automatica:** O realm `tessera` e importado automaticamente a partir do ficheiro `infra/keycloak/realm-export.json` na primeira execucao

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

O sistema define tres roles que correspondem aos tres perfis de utilizador identificados na proposta do projeto:

### admin — Administrador do Clube

Acesso total a gestao do clube. Permissoes:

- Gestao de jogadores e planteis
- Criacao e gestao de jogos
- Configuracao de eventos de bilheteira (precos, estados)
- Consulta de relatorios de bilheteira e estatisticas
- Gestao de membros staff do clube

### staff — Staff do Clube

Operacoes no dia de jogo. Permissoes:

- Validacao de bilhetes na entrada do recinto (leitura QR code)
- Preenchimento da ficha tecnica do jogo em tempo real
- Registo de ocorrencias (golos, cartoes, substituicoes)
- Consulta de jogos e fichas tecnicas

### fan — Adepto

Utilizador final da plataforma. Permissoes:

- Compra de bilhetes digitais
- Consulta de jogos e resultados
- Visualizacao de fichas tecnicas e estatisticas
- Gestao do perfil pessoal

**Role por defeito:** Quando um novo utilizador se regista, recebe automaticamente o role `fan`.

## Clients (Aplicacoes)

### tessera-web — React SPA

| Propriedade | Valor |
|-------------|-------|
| Tipo | Publico (sem secret) |
| Protocolo | OpenID Connect |
| Fluxo | Authorization Code + PKCE |
| Redirect URIs | `http://localhost:5173/*`, `http://localhost:8000/*` |
| Web Origins | `http://localhost:5173`, `http://localhost:8000` |
| Token Lifespan | 30 minutos |

Utilizado pela aplicacao web React. Como client publico, nao armazena credenciais no browser — utiliza PKCE (Proof Key for Code Exchange) para seguranca adicional no fluxo de autorizacao.

### tessera-android — Aplicacao Android

| Propriedade | Valor |
|-------------|-------|
| Tipo | Publico (sem secret) |
| Protocolo | OpenID Connect |
| Fluxo | Authorization Code + PKCE |
| Redirect URIs | `tessera://callback/*` |
| Token Lifespan | 60 minutos |

Utilizado pela aplicacao movel Android. O redirect URI usa um custom scheme (`tessera://`) para interceptar o callback de autenticacao na app.

### tessera-bff — Servico BFF

| Propriedade | Valor |
|-------------|-------|
| Tipo | Confidencial (com secret) |
| Protocolo | OpenID Connect |
| Service Account | Ativado |
| Secret | `change-me-in-production` |

Utilizado pelo BFF para comunicacao servico-a-servico. Como client confidencial, usa um secret para autenticar-se diretamente com o Keycloak (Client Credentials Grant).

## Utilizadores de Teste

Para desenvolvimento e testes, o realm inclui tres utilizadores pre-configurados:

| Username | Password | Email | Role |
|----------|----------|-------|------|
| `admin` | `admin` | admin@tessera.pt | admin |
| `staff` | `staff` | staff@tessera.pt | staff |
| `adepto` | `adepto` | adepto@tessera.pt | fan |

**Nota:** Estas credenciais sao apenas para desenvolvimento. Em producao, os utilizadores devem ser criados com passwords seguras.

## Protocol Mappers

Os clients `tessera-web` e `tessera-android` incluem um protocol mapper customizado que adiciona os realm roles ao token JWT:

```json
{
  "name": "realm-roles",
  "protocolMapper": "oidc-usermodel-realm-role-mapper",
  "config": {
    "claim.name": "roles",
    "access.token.claim": "true",
    "id.token.claim": "true",
    "userinfo.token.claim": "true"
  }
}
```

Isto garante que os roles do utilizador aparecem no claim `roles` do token, permitindo ao frontend e ao BFF verificar permissoes sem consultas adicionais ao Keycloak.

### Exemplo de Token JWT (payload)

```json
{
  "sub": "a1b2c3d4-...",
  "preferred_username": "admin",
  "email": "admin@tessera.pt",
  "realm_access": {
    "roles": ["admin", "default-roles-tessera"]
  },
  "roles": ["admin"]
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

O ficheiro `infra/keycloak/realm-export.json` e importado automaticamente quando o container inicia pela primeira vez. Para re-importar (ex: apos alteracoes ao ficheiro):

1. Parar e remover o container Keycloak
2. Reiniciar — o Keycloak ira re-importar o ficheiro

```powershell
docker compose stop keycloak
docker compose rm -f keycloak
docker compose up -d keycloak
```

Ou utilizar o script `scripts/run/reset.ps1` que faz reset completo.
