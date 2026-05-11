# Seguranca

## Visao geral

A autenticacao e autorizacao do Tessera assentam em **OAuth 2.0 / OpenID
Connect** com **Keycloak** como Identity Provider. O sistema segue o
padrao **JWT pass-through**:

1. O cliente (SPA, Android) faz login no Keycloak e recebe um JWT.
2. O cliente envia o JWT no cabecalho `Authorization: Bearer <token>` em
   cada pedido para o BFF.
3. O BFF **valida** o token (assinatura via JWKS) e **encaminha** o
   `Authorization` para o servico downstream.
4. O servico downstream (ex.: match-service) **re-valida** o mesmo
   token e aplica controlo de acessos por papel via `@PreAuthorize`.

Esta abordagem garante **defesa em profundidade**: cada salto da
cadeia verifica o token, pelo que mesmo um ataque que comprometa a
rede interna do Docker nao consegue chamar os servicos sem JWT valido.

```
Browser/App ──JWT──> NGINX :8000 ──> BFF :8080 ──Authorization fwd──> match-service :8082
                                       (valida JWT)                     (re-valida JWT
                                                                         + check role)
```

## Padroes considerados

| Padrao | Descricao | Decisao |
|--------|-----------|---------|
| **JWT pass-through** | Cada servico valida o JWT independentemente | **Escolhido** |
| Header injection | BFF extrai claims e injeta `X-User-Roles` para servicos | Rejeitado: requer rede 100% confiavel |
| Token exchange | BFF troca JWT do utilizador por JWT machine-to-machine | Excessivo para a escala do projeto |

O JWT pass-through e o padrao mais standards-based (cada servico e
um OAuth2 resource server independente) e nao requer que o BFF guarde
estado nem que os servicos confiem cegamente em headers.

## Papeis (roles)

Os papeis sao definidos no realm `tessera` do Keycloak. Cada utilizador
tem um e apenas um papel:

| Papel | Descricao | Uso tipico |
|-------|-----------|-----------|
| `admin` | Administrador do clube | Gestao de clubes, equipas, jogadores, jogos, eventos de bilheteira |
| `staff` | Staff do clube | Validacao de bilhetes no dia de jogo, preenchimento de ficha tecnica |
| `fan` | Adepto | Compra e consulta de bilhetes |

## Mapeamento de papeis

O JWT emitido pelo Keycloak inclui um claim `roles` (custom mapper
configurado no `tessera-web` e `tessera-android` clients):

```json
{
  "sub": "a1b2-...",
  "preferred_username": "admin",
  "email": "admin@tessera.pt",
  "realm_access": { "roles": ["admin", "default-roles-tessera"] },
  "roles": ["admin"]
}
```

Os servicos backend leem **primeiro** o claim `roles` (filtrado para
apenas `admin`, `staff`, `fan`) e fazem fallback para
`realm_access.roles` caso o claim custom nao esteja presente. Cada
papel encontrado e exposto a Spring Security com o prefixo `ROLE_`
(ex: `ROLE_admin`), permitindo expressoes como
`@PreAuthorize("hasRole('admin')")`.

A configuracao reside em
`SecurityConfig.kt` (match-service e bff-service), na funcao
`realmRolesConverter()`.

## Resource server config

Tanto o **bff-service** como o **match-service** sao configurados
como OAuth2 resource servers via Spring Boot:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://keycloak:8180/realms/tessera/protocol/openid-connect/certs}
```

### Porque `jwk-set-uri` e nao `issuer-uri`?

O Keycloak emite tokens com `iss = http://localhost:8180/realms/tessera`
(host port) mas, dentro da rede Docker, o servico chega ao Keycloak
em `http://keycloak:8180`. Se usassemos `issuer-uri`, o Spring
compararia o claim `iss` com este valor e o token seria rejeitado.

Com `jwk-set-uri`, o Spring valida apenas a assinatura criptografica
do JWT (via JWKS endpoint) e ignora a comparacao do issuer.
Alternativamente, podia-se configurar `KC_HOSTNAME` no Keycloak para
emitir tokens com um issuer estatico, mas a abordagem JWKS e mais
simples e suficiente para desenvolvimento.

## Endpoints publicos vs autenticados

| Endpoint | Acesso | Notas |
|----------|--------|-------|
| `GET /api/v1/clubs`, `/venues`, `/teams`, `/players`, `/matches` | Publico | Catalogo aberto a adeptos |
| `POST/PATCH/DELETE` em qualquer recurso | Autenticado | Token JWT obrigatorio |
| Acoes em `/sheet/lineup`, `/sheet/occurrences`, `/sheet/lock` | `staff` ou `admin` | Operacoes no dia de jogo |
| `/sheet/unlock` | `admin` | Reabertura manual de ficha |
| Bilheteira (futuro) | `fan` para compra, `admin` para gestao | Em desenvolvimento |

Os enforcement points sao:

1. **BFF (`SecurityConfig`)** — bloqueia pedidos sem token nos
   endpoints que requerem auth (devolve 401 Problem JSON).
2. **Match-service (`SecurityConfig` + `@PreAuthorize`)** —
   re-valida o JWT e verifica o papel especifico exigido por cada
   endpoint (devolve 403 Problem JSON caso o papel nao bata certo).

## Erros 401 / 403

Quando a autenticacao ou autorizacao falham, a resposta segue o
formato **RFC 7807 Problem Details** (`application/problem+json`):

```json
{
  "type": "https://tessera/api/errors/forbidden",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/clubs"
}
```

Implementacao no `SecurityConfig`:

- `AuthenticationEntryPoint` customizado para 401
- `AccessDeniedHandler` customizado para 403

Estes handlers correm dentro do filter chain do Spring Security, antes
do `@RestControllerAdvice` ser chamado — daiq necessario configura-los
explicitamente para produzir Problem JSON em vez do default Spring
(HTML).

## Utilizadores de teste (desenvolvimento)

Definidos no `keycloak/realm-export.json`, importados automaticamente
quando o container Keycloak arranca pela primeira vez:

| Username | Password | Role |
|----------|----------|------|
| `admin`  | `admin`  | admin |
| `staff`  | `staff`  | staff |
| `adepto` | `adepto` | fan |

**Atencao:** estas credenciais sao **apenas para desenvolvimento**.
Em producao, devem ser substituidas por contas reais com palavras
passe fortes (idealmente sem auto-importacao do realm — gerir via
consola Keycloak).

## Obter um JWT (manual)

```powershell
function Get-JWT($username, $password) {
    $r = Invoke-RestMethod -Method POST `
      -Uri "http://localhost:8180/realms/tessera/protocol/openid-connect/token" `
      -ContentType "application/x-www-form-urlencoded" `
      -Body @{
          client_id  = "tessera-web"
          username   = $username
          password   = $password
          grant_type = "password"
      }
    return $r.access_token
}

$adminToken = Get-JWT "admin" "admin"
```

Os ficheiros `.http` em `docs/http-tests/` automatizam isto:
`00-auth.http` obtem tokens para admin, staff e adepto e guarda-os
como variaveis globais usadas pelos restantes ficheiros.

## CORS

O BFF e match-service permitem origens do frontend de desenvolvimento:

- `http://localhost:5173` (Vite dev server)
- `http://localhost:8000` (NGINX em ambiente integrado)

Em producao, esta lista deve ser restrita ao dominio publico
real (ex: `https://tessera.example.pt`).

## Trabalho futuro

- [ ] Refresh tokens automaticos no SPA via biblioteca oficial
      (`keycloak-js`)
- [ ] Login flow PKCE para Android (custom scheme redirect
      `tessera://callback`)
- [ ] Auditoria de acoes sensiveis (criacao/eliminacao de jogos,
      lock/unlock de fichas) com log estruturado
- [ ] Migrar Keycloak de H2 (default em `start-dev`) para PostgreSQL
      em producao
- [ ] HTTPS no NGINX (Let's Encrypt ou certificado fornecido pelo
      clube)
