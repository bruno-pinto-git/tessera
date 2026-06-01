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

Os papeis sao definidos no realm `tessera` do Keycloak. Existem **quatro**
papeis de realm. Um utilizador pode acumular papeis, e os papeis scoped
(`club-manager`, `staff`) sao delimitados aos clubes a que o utilizador
pertence via grupos `/clubs/<id>/managers` e `/clubs/<id>/staff`:

| Papel | Descricao | Uso tipico |
|-------|-----------|-----------|
| `platform-admin` | Administrador da plataforma/sistema | Ve todos os clubes, gere o catalogo, cria/edita utilizadores e atribui gestores de clube |
| `club-manager` | Gestor scoped a um ou mais clubes (via grupo `/clubs/<id>/managers`) | Gere equipas, jogadores, jogos em casa, bilheteira desses jogos e staff (members) do clube |
| `staff` | Staff scoped a um ou mais clubes (via grupo `/clubs/<id>/staff`) | Validacao de bilhetes no dia de jogo, preenchimento de ficha tecnica |
| `fan` | Adepto | Consulta de catalogo, compra e consulta de bilhetes |

O papel por defeito do realm (`default-roles-tessera`) e um papel
composto que inclui `fan`, pelo que qualquer utilizador auto-registado
(formulario de registo ou login social Google) fica efetivamente com o
papel `fan`.

## Mapeamento de papeis

O JWT emitido pelo Keycloak inclui um claim `roles` (custom mapper
configurado no `tessera-web` e `tessera-android` clients):

```json
{
  "sub": "a1b2-...",
  "preferred_username": "admin",
  "email": "admin@tessera.pt",
  "realm_access": { "roles": ["platform-admin", "default-roles-tessera"] },
  "roles": ["platform-admin"],
  "groups": ["/clubs/1/managers"]
}
```

Alem do claim `roles`, os clients `tessera-web` e `tessera-android`
incluem mappers para `sub` (id do utilizador) e `groups` (pertenca a
grupos, com path completo — usado para resolver o scope de
`club-manager`/`staff` por clube).

Os servicos backend leem **primeiro** o claim `roles` (filtrado para
apenas `platform-admin`, `club-manager`, `staff`, `fan`) e fazem
fallback para `realm_access.roles` caso o claim custom nao esteja
presente. Cada papel encontrado e exposto a Spring Security com o
prefixo `ROLE_` (ex: `ROLE_platform-admin`), permitindo expressoes como
`@PreAuthorize("hasRole('platform-admin')")`.

A configuracao reside em `SecurityConfig.kt` na funcao
`realmRolesConverter()` (com o set `TESSERA_ROLES`), presente em
**bff-service**, **match-service**, **ticket-service** e
**statistics-service**.

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
| `POST /api/v1/tickets`, `/{id}/pay`, `GET /tickets/mine` | Autenticado (tipicamente `fan`) | Compra e pagamento do proprio bilhete |
| `POST /api/v1/tickets/validate`, `GET /api/v1/tickets?eventId=` | `staff` ou `admin` | Validacao na porta (Android) e listagem por evento |
| `/api/v1/users/**` (search/create/update/delete) | `platform-admin` | Gestao de utilizadores (ver seccao abaixo) |

Os enforcement points sao:

1. **BFF (`SecurityConfig`)** — bloqueia pedidos sem token nos
   endpoints que requerem auth (devolve 401 Problem JSON).
2. **Match-service (`SecurityConfig` + `@PreAuthorize`)** —
   re-valida o JWT e verifica o papel especifico exigido por cada
   endpoint (devolve 403 Problem JSON caso o papel nao bata certo).

## Matriz de autorizacao de escritas (escopo por clube)

As escritas que tocam dados de um clube especifico nao sao gated apenas
por papel — sao **scoped ao clube** via o bean `@clubAuthz`
(`ClubAuthorizationService`). Este bean le o claim `groups` do JWT
(`/clubs/<id>/managers` e `/clubs/<id>/staff`, extraido pelo
`ClubMembershipExtractor`) e decide se o utilizador pertence ao clube
alvo. Um `platform-admin` passa sempre; caso contrario e preciso a
pertenca de grupo adequada — falha de escopo devolve **403**.

| Recurso | Quem pode escrever | Verificacao |
|---------|--------------------|-------------|
| **Clubs** | `platform-admin` | recurso de catalogo/plataforma |
| **Venues** | `platform-admin` | recurso de catalogo/plataforma |
| **Teams** | manager do **clube** (ou admin) | `@clubAuthz.canManageClub` / `canManageTeam` |
| **Players** | manager do **clube** (ou admin) | `@clubAuthz.canManageTeam` / `canManagePlayer` |
| **Matches** (create/update/delete) | manager do **clube da equipa da casa** (ou admin) | `@clubAuthz.canManageTeam(homeTeamId)` no create; `canManageMatch` no update/delete |
| **Match sheet** (lineup/occurrences/lock) | manager **ou** staff de um dos dois clubes envolvidos (ou admin); `unlock` so admin | `@clubAuthz.canEditSheet` |
| **Box office / Events** (abrir bilheteira) | manager do **clube da casa** do jogo (ou admin) | check in-code no ticket-service (ver abaixo) |
| **Members** (`/clubs/{id}/members`) | manager do **clube** — apenas role STAFF (ou admin) | `@clubAuthz.canManageClub`; non-admins restritos a STAFF server-side |
| **Users** (`/api/v1/users`) | `platform-admin` | `hasRole('platform-admin')` |

Notas:

- **Matches** sao geridos pelo clube **anfitriao**: criar/editar/eliminar
  um jogo exige ser manager do clube da equipa da casa.
  `canManageMatch` resolve o clube da casa a partir de `homeTeamId`.
  (Antes, `PATCH /matches/{id}` usava um `hasAnyRole` **sem escopo** que
  permitia qualquer manager/staff editar qualquer jogo — corrigido.)
- **Box office:** abrir bilheteira para um jogo (`POST /api/v1/events` no
  ticket-service) deixou de ser exclusivo do admin. Agora exige apenas
  `isAuthenticated()` + um check em codigo: o admin abre para qualquer
  jogo; um manager so para jogos cujo **clube da casa** ele gere. O
  ticket-service resolve o clube da casa chamando o match-service
  (`GET /api/v1/matches/{id}` → `homeClubId`) e le os clubes geridos do
  claim `groups` do JWT.
- **Members:** gerir managers/staff de um clube deixou de ser exclusivo
  do admin — um manager gere o **seu** clube, mas so pode adicionar/
  remover **staff** (nunca managers). Pode tambem criar um novo
  utilizador staff inline (aprovisionado no Keycloak com password
  temporaria). A lista global `/api/v1/users` permanece so `platform-admin`.

## Login social (Google)

O realm tem configurado um identity provider Google (alias `google`,
`providerId` `google`) para login social. Esta ativo um
**identity provider mapper** do tipo `oidc-hardcoded-role-idp-mapper`
que atribui automaticamente o papel de realm `fan` a qualquer
utilizador brokered via Google, com `syncMode = FORCE` (reaplicado em
cada login). Assim, um utilizador que entre com a sua conta Google fica
imediatamente como adepto, sem passo de atribuicao manual.

**Nota sobre utilizadores brokered:** apagar um utilizador criado via
Google **nao** e um banimento permanente — no proximo login Google o
Keycloak volta a aprovisionar a conta. Para bloquear efetivamente um
utilizador, deve-se **desativar** (disable) a conta em vez de a apagar.

## Gestao de utilizadores (platform-admin)

Para alem da consola do Keycloak, a plataforma expoe endpoints proprios
de gestao de utilizadores. **Todos exigem o papel `platform-admin`**
(`@PreAuthorize("hasRole('platform-admin')")`).

O **match-service** (`com.tessera.match.iam.UserController`,
`/api/v1/users`) e o ponto de implementacao; o **BFF**
(`UserProxyController`) reencaminha estes pedidos. Internamente, o
match-service usa o `KeycloakAdminClient`, que se autentica com a conta
de servico do client confidencial `tessera-bff` (grant
`client_credentials`) para chamar a **Keycloak Admin REST API**.

| Metodo | Endpoint | Acao |
|--------|----------|------|
| GET | `/api/v1/users?search=&first=&max=` | Pesquisa utilizadores |
| GET | `/api/v1/users/{id}` | Detalhe de um utilizador |
| POST | `/api/v1/users` | Cria utilizador e atribui `club-manager` ou `staff` |
| PUT | `/api/v1/users/{id}` | Atualiza perfil, reatribui papel manuvel e/ou forca reset de password |
| DELETE | `/api/v1/users/{id}` | Elimina utilizador |

Notas de comportamento:

- Na criacao (POST), apenas os papeis `club-manager` ou `staff` podem
  ser atribuidos (`platform-admin` vem do realm-export; `fan` vem do
  papel composto por defeito). A password e definida como **temporaria**
  (`TEMPORARY`), pelo que o utilizador e obrigado a escolher uma nova no
  primeiro login.
- Na atualizacao (PUT), so se reconcilia o papel manuvel
  (`club-manager`/`staff`); `platform-admin`, `fan` e papeis por defeito
  ficam intactos. Quando `forcePasswordReset = true`, e adicionada a
  required action `UPDATE_PASSWORD`.
- A pagina `/admin/users` do frontend usa estes endpoints para
  criar/editar/ativar-desativar/forcar reset/eliminar e mostra o papel
  de cada utilizador.

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

Definidos no `infra/keycloak/realm-export.json`, importados automaticamente
quando o container Keycloak arranca pela primeira vez:

| Username | Password | Role |
|----------|----------|------|
| `admin`  | `admin`  | platform-admin |
| `gestor` | `gestor` | club-manager |
| `staff`  | `staff`  | staff |
| `adepto` | `adepto` | fan |

Existe ainda o utilizador `service-account-tessera-bff` (conta de
servico do client confidencial `tessera-bff`), nao destinado a login
interativo.

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
`00-auth.http` obtem tokens para admin, gestor, staff e adepto e
guarda-os como variaveis globais usadas pelos restantes ficheiros.

## CORS

Os quatro servicos (bff, match, ticket, statistics) permitem origens do
frontend de desenvolvimento:

- `http://localhost:5173` (Vite dev server)
- `http://localhost:8000` (NGINX em ambiente integrado)

Em producao, esta lista deve ser restrita ao dominio publico
real (ex: `https://tessera.example.pt`).

**`allowedMethods` tem de incluir `PATCH`.** O `CorsConfig` de cada
servico lista explicitamente `GET, POST, PUT, PATCH, DELETE`. Omitir
`PATCH` faz com que **todas** as edicoes parciais (matches, teams,
players, clubs, venues) sejam rejeitadas com `403 "Invalid CORS request"`
**antes** de chegarem ao controller — para qualquer papel, incluindo
admin. O preflight `OPTIONS` falha silenciosamente porque o metodo nao
consta da lista permitida.

## Trabalho futuro

- [ ] Refresh tokens automaticos no SPA via biblioteca oficial
      (`keycloak-js`)
- [ ] Auditoria de acoes sensiveis (criacao/eliminacao de jogos,
      lock/unlock de fichas) com log estruturado
- [ ] Migrar Keycloak de H2 (default em `start-dev`) para PostgreSQL
      em producao
- [ ] HTTPS no NGINX (Let's Encrypt ou certificado fornecido pelo
      clube)
