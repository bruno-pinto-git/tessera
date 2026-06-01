# match-service

O microsservico responsavel pela atividade desportiva: clubes, equipas,
jogadores, recintos, jogos e fichas tecnicas. E o servico de dominio mais
rico do Tessera. Aloja tambem a gestao de utilizadores (IAM) atraves de
um wrapper sobre a Keycloak Admin REST API (ver seccao **IAM / Gestao de
utilizadores**).

- **Porta:** 8082
- **Base de dados:** `tessera_matches` (PostgreSQL 16)
- **Stack:** Spring Boot 3.4.4, Kotlin 1.9.25, JPA + Hibernate,
  Flyway, OAuth2 Resource Server
- **Base path:** `/api/v1`

## Recursos

| Recurso | Tabela | Endpoints | Notas |
|---------|--------|-----------|-------|
| **Club** | `club` | `/clubs` + `/clubs/{id}` | Soft delete |
| **Venue** | `venue` | `/venues` + `/venues/{id}` | Soft delete |
| **Team** | `team` | `/clubs/{clubId}/teams` + `/teams/{id}` | FK → club; soft delete |
| **Player** | `player` | `/teams/{teamId}/players` + `/players/{id}` | FK → team; soft delete |
| **Match** | `match` | `/matches` + `/matches/{id}` | FKs → team x2, venue; soft delete |
| **MatchSheet** | `match_sheet` + `lineup_entry` + `occurrence` | `/matches/{id}/sheet/...` | Lazy create; lock/unlock |
| **User** | _(sem tabela — Keycloak)_ | `/users` + `/users/{id}` | Wrapper Keycloak Admin API; so `platform-admin` |
| **ClubMembership** | _(sem tabela — grupos Keycloak)_ | `/clubs/{clubId}/members` + `/clubs/{clubId}/members/{userId}` | manager/staff por grupo; admin OU manager do clube (`@clubAuthz.canManageClub`) |

## Convencoes da API

| | |
|---|---|
| Path versionado | `/api/v1/...` |
| JSON casing | camelCase |
| Data/hora | ISO 8601 com offset (`2026-09-15T20:30:00Z`) |
| Paginacao | `?page=0&size=20&sort=field,asc` |
| Envelope de pagina | `{ content, page, size, totalElements, totalPages }` |
| Erros | RFC 7807 Problem Details (`application/problem+json`) |
| Auth | Bearer JWT do Keycloak (excepto GETs publicos) |
| Roles | `platform-admin`, `club-manager`, `staff`, `fan` (realm roles) |

### Modelo de autorizacao

A maioria das escritas e gated por role de realm via `@PreAuthorize`:

- **`platform-admin`** — acesso total (clubes, venues, matches, IAM).
- **`club-manager` / `staff`** — acesso *com escopo* ao seu clube. As
  escritas sobre equipas, jogadores e fichas usam o bean
  `@clubAuthz` (`ClubAuthorizationService`) que valida, alem do role,
  a pertenca do utilizador ao clube em causa (claim `groups` do JWT,
  extraido por `ClubMembershipExtractor`). Um `platform-admin` passa
  sempre; caso contrario, e preciso uma `ClubMembership` (MANAGER para
  gerir equipas/jogadores, MANAGER ou STAFF para editar a ficha) do
  clube alvo. Falha de escopo devolve **403**.

  Verificacoes expostas pelo `@clubAuthz`:

  - `canManageClub(clubId)` — admin ou MANAGER do clube.
  - `canManageTeam(teamId)` / `canManagePlayer(playerId)` — resolve o
    clube via team/player e delega em `canManageClub`.
  - `canManageMatch(matchId)` — admin ou MANAGER do clube da **equipa da
    casa** do jogo (resolve `homeTeamId` → `clubId`). Mais restrito que
    `canEditSheet`: gerir o jogo em si pertence ao anfitriao.
  - `canEditSheet(matchId)` — admin ou manager/staff de **qualquer** dos
    dois clubes envolvidos.

## Diagrama de dominio

```
Club 1───* Team 1───* Player
 │         │
 │         └─────────────┐
 │                       │
 │   Venue              ▼
 │     │       Match (homeTeamId, awayTeamId, venueId, status, ...)
 │     │            │
 │     └─ FK ───────┤
 │                  │
 │                  └─ 1:1 ─ MatchSheet (locked, lockedAt)
 │                                │
 │                                ├───* LineupEntry (playerId, teamId, shirtNumber, role)
 │                                │
 │                                └───* Occurrence (minute, type, playerId, replacedPlayerId?)
```

Todos os recursos usam **soft delete** (coluna `deleted_at`) para
preservar referencias historicas. Lineup entries e occurrences sao
hard-deleted porque o lineup e reconstruido livremente enquanto a
sheet esta desbloqueada.

## Club

Pessoa juridica que detem uma ou mais equipas. Soft-deletado para
preservar matches historicos.

```
Club { id, name, foundedYear?, crestUrl?, createdAt, deletedAt? }
```

**Endpoints:**

| Method | Path | Role | Descricao |
|--------|------|------|-----------|
| GET | `/clubs` | publico | Lista paginada, suporta filtro `?name=...` |
| GET | `/clubs/{id}` | publico | Detalhe |
| POST | `/clubs` | platform-admin | Cria |
| PATCH | `/clubs/{id}` | platform-admin | Atualizacao parcial |
| DELETE | `/clubs/{id}` | platform-admin | Soft delete |

**Constraints:** `name` UNIQUE entre clubes ativos (case-insensitive).

## Venue

Estadio ou recinto onde se disputam jogos. Independente de clube
(um estadio pode ser partilhado).

```
Venue { id, name, capacity, address?, createdAt, deletedAt? }
```

| Method | Path | Role |
|--------|------|------|
| GET | `/venues` | publico |
| GET | `/venues/{id}` | publico |
| POST | `/venues` | platform-admin |
| PATCH | `/venues/{id}` | platform-admin |
| DELETE | `/venues/{id}` | platform-admin |

**Constraints:** `name` UNIQUE entre venues ativos; `capacity` ∈ [0, 200000].

## Team

Equipa de um clube, identificada pela categoria (escalao + genero).
Um clube pode ter no maximo uma equipa ativa por categoria.

```
Team { id, clubId, category, createdAt, deletedAt? }
```

**Categorias:** SENIOR_M, SENIOR_F, SUB_23, SUB_19, SUB_17, SUB_15,
SUB_13, SUB_11, SUB_9, SUB_7, VETERANS, OTHER.

| Method | Path | Role |
|--------|------|------|
| GET | `/clubs/{clubId}/teams` | publico |
| GET | `/teams/{id}` | publico |
| POST | `/clubs/{clubId}/teams` | platform-admin / club-manager do clube |
| PATCH | `/teams/{id}` | platform-admin / club-manager do clube |
| DELETE | `/teams/{id}` | platform-admin / club-manager do clube |

Escritas gated por `@clubAuthz.canManageClub` / `canManageTeam`
(escopo por clube — ver **Modelo de autorizacao**).

**Constraints:** unique index parcial `(club_id, category) WHERE deleted_at IS NULL`.
Tentativa de duplicar categoria devolve **409**.

## Player

Jogador pertencente a uma equipa. Soft-deletado para preservar
referencias em fichas tecnicas antigas.

```
Player {
  id, teamId, firstName, lastName,
  birthdate?, nationality? (ISO 3166-1 alpha-3),
  position (GK|DF|MF|FW),
  shirtNumber?,
  photoUrl?, dominantFoot? (LEFT|RIGHT|BOTH),
  height?, weight?,
  status (ACTIVE|INJURED|SUSPENDED),
  createdAt, deletedAt?
}
```

| Method | Path | Role | Notas |
|--------|------|------|-------|
| GET | `/teams/{teamId}/players` | publico | Paginado, ordenado por shirt |
| GET | `/clubs/{clubId}/players` | publico | **Squad completo do clube** (todas as equipas ativas), paginado, ordem alfabetica |
| GET | `/players/{id}` | publico | |
| POST | `/teams/{teamId}/players` | platform-admin / club-manager | `@clubAuthz.canManageTeam` |
| PATCH | `/players/{id}` | platform-admin / club-manager | `@clubAuthz.canManagePlayer` |
| DELETE | `/players/{id}` | platform-admin / club-manager | `@clubAuthz.canManagePlayer` |

**Constraints:** unique index parcial
`(team_id, shirt_number) WHERE deleted_at IS NULL AND shirt_number IS NOT NULL`.
Ranges validados: `shirt_number` 1-99, `height` 100-250 cm,
`weight` 30-200 kg.

**Ordenacao da listagem:** por `shirt_number` ascendente (nulls last),
depois `last_name`.

## Match

Jogo entre duas equipas, com ciclo de vida controlado.

```
Match {
  id,
  homeTeamId, awayTeamId, venueId?,
  kickoffAt,
  status (SCHEDULED|LIVE|FINISHED|POSTPONED|ABANDONED),
  homeScore?, awayScore?,
  refereeName?,
  createdAt, deletedAt?
}
```

| Method | Path | Role | Notas |
|--------|------|------|-------|
| GET | `/matches` | publico | Filtros: `?from`, `?to`, `?status`, `?clubId` |
| GET | `/matches/{id}` | publico | |
| POST | `/matches` | platform-admin / manager do clube da casa | `@clubAuthz.canManageTeam(#req.homeTeamId)`; sempre criado em SCHEDULED |
| PATCH | `/matches/{id}` | platform-admin / manager do clube da casa | `@clubAuthz.canManageMatch`; status transitions e scores |
| DELETE | `/matches/{id}` | platform-admin / manager do clube da casa | `@clubAuthz.canManageMatch`; soft delete |

Gerir um jogo (criar/editar/eliminar) e privilegio do **clube
anfitriao**: exige ser `platform-admin` ou MANAGER do clube da equipa da
casa (`homeTeamId`). Isto e mais restrito que a edicao da ficha tecnica
(`canEditSheet`), que aceita managers/staff de qualquer dos dois clubes.

> **Nota historica:** ate recentemente `PATCH /matches/{id}` usava um
> `hasAnyRole('platform-admin','club-manager','staff')` **sem escopo** —
> qualquer manager/staff conseguia editar qualquer jogo — e POST/DELETE
> eram so admin. O escopo por clube da casa fechou esse buraco.

A `MatchResponse` expoe `homeClubId` e `awayClubId` (clube de cada
equipa, resolvidos por `MatchService.clubIdsForTeams` numa unica query;
`null` se a equipa nao for resolvivel). Estes campos permitem ao
frontend e ao ticket-service saber a que clube pertence cada jogo sem
chamadas extra.

### Filtro `?clubId`

Usa `JpaSpecificationExecutor` com subquery: o match envolve uma equipa
do clube se `homeTeamId` ou `awayTeamId` pertencer a uma equipa ativa
desse clube. Implementacao em `MatchRepository.kt`
(`MatchSpecs.involvesClub`).

### Status state machine

```
   ┌──────────┐
   │SCHEDULED ├──────────────┐
   └─┬─┬──┬───┘              │
     │ │  │                  ▼
     │ │  │             ┌──────────┐
     │ │  │             │POSTPONED │
     │ │  │             └────┬─────┘
     │ │  │                  │
     │ │  │   ┌──────────────┘  (volta a SCHEDULED ao reagendar)
     │ │  ▼   ▼
     │ │ ┌───────┐       ┌──────────┐
     │ │ │ LIVE  ├──────▶│FINISHED  │ (terminal — scores obrigatorios)
     │ │ └───┬───┘       └──────────┘
     │ │     │
     │ └─────┴──▶┌──────────┐
     │           │ABANDONED │ (terminal)
     │           └──────────┘
     │
     └────────▶┌──────────┐
               │CANCELLED │ (terminal — desmarcado antes do kickoff)
               └──────────┘
```

Validacoes:

- `home_team_id <> away_team_id`
- `FINISHED` exige `homeScore != null AND awayScore != null`
- Transicoes invalidas devolvem **409**
- Quando entra em estado terminal (FINISHED/ABANDONED/POSTPONED/CANCELLED),
  a `MatchSheet` correspondente (se existir) e auto-lockada

### Distincao entre estados terminais

| Estado | Significado |
|--------|-------------|
| `FINISHED` | Jogo decorreu normalmente e completou-se. Scores registados. |
| `ABANDONED` | Jogo comecou mas teve de ser interrompido (chuva, problemas no estadio). |
| `CANCELLED` | Jogo desmarcado antes do kickoff, sem intencao de reagendar. |
| `POSTPONED` | Adiado — pode voltar a `SCHEDULED` quando se marcar nova data. |

## MatchSheet (ficha tecnica)

Documento que regista a composicao das equipas e os eventos do jogo.
Criada **lazy**: o primeiro `GET /matches/{id}/sheet` cria uma sheet
vazia se ainda nao existir.

```
MatchSheet { id, matchId (UNIQUE), locked, lockedAt?, createdAt }
  └─ LineupEntry   (PK: matchSheetId + playerId)
                   { teamId, shirtNumber?, role (STARTER|SUBSTITUTE) }
  └─ Occurrence    { id, minute (0-200), type, teamId,
                     playerId, replacedPlayerId? }
                   type ∈ { GOAL, OWN_GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION }
```

### Edicao

A sheet so e editavel se:

1. `locked = false` **E**
2. `match.status ∈ { SCHEDULED, LIVE }`

Se qualquer destas condicoes falhar, a operacao devolve **409**.

### Endpoints

| Method | Path | Role |
|--------|------|------|
| GET | `/matches/{id}/sheet` | publico |
| POST | `/matches/{id}/sheet/lineup` | `@clubAuthz.canEditSheet` |
| PATCH | `/matches/{id}/sheet/lineup/{playerId}` | `@clubAuthz.canEditSheet` |
| DELETE | `/matches/{id}/sheet/lineup/{playerId}` | `@clubAuthz.canEditSheet` |
| POST | `/matches/{id}/sheet/occurrences` | `@clubAuthz.canEditSheet` |
| DELETE | `/matches/{id}/sheet/occurrences/{occId}` | `@clubAuthz.canEditSheet` |
| POST | `/matches/{id}/sheet/lock` | `@clubAuthz.canEditSheet` |
| POST | `/matches/{id}/sheet/unlock` | **platform-admin** |

`canEditSheet` permite o `platform-admin` ou qualquer manager/staff de
um dos dois clubes envolvidos no jogo (ver **Modelo de autorizacao**).

### Tipos de occurrence

`GOAL`, `OWN_GOAL`, `YELLOW_CARD`, `RED_CARD`, `SUBSTITUTION`, `FOUL`.

### Validacoes de lineup (regras de elenco)

| Regra | Limite | Resposta se violada |
|-------|--------|---------------------|
| Max titulares por equipa | 11 STARTER | 409 |
| Max suplentes por equipa | 12 SUBSTITUTE | 409 |

Aplicado em `POST /sheet/lineup` e em `PATCH /sheet/lineup/{playerId}`
quando o role muda. Mover SUBSTITUTE → STARTER se ja ha 11 STARTERs e
rejeitado.

### Validacoes de occurrences

- O `playerId` tem de estar no lineup da sheet
- Se `type = SUBSTITUTION`: `replacedPlayerId` obrigatorio, ambos os
  jogadores na mesma equipa
- Se `type != SUBSTITUTION`: `replacedPlayerId` proibido
- `playerId != replacedPlayerId`
- `minute ∈ [0, 200]`
- **Max 5 substituicoes por equipa por jogo** (FIFA modern) — 409
- **Bloqueio por cartao vermelho**: depois de receber `RED_CARD`, o
  jogador nao pode ser autor de occurrences subsequentes — 409

## IAM / Gestao de utilizadores

O pacote `com.tessera.match.iam` adiciona ao match-service a gestao de
utilizadores e de pertencas a clube. Nao ha persistencia Tessera-side: e
um wrapper fino sobre a **Keycloak Admin REST API**. O Keycloak continua a
ser a fonte da verdade para utilizadores, roles e grupos.

### KeycloakAdminClient

Cliente REST minimo (`KeycloakAdminClient.kt`) para a Admin API. Autentica
com a **service account do client confidencial `tessera-bff`** via grant
`client_credentials` e mantem o token em cache ate perto da expiracao
(renova 30s antes). Operacoes expostas (so as que o Tessera precisa):

- **Grupos:** `findGroupByPath`, `createTopLevelGroup`, `createChildGroup`,
  `deleteGroup`, `listGroupMembers`, `addUserToGroup`, `removeUserFromGroup`.
- **Utilizadores:** `searchUsers` (paginado por `first`/`max`), `getUser`,
  `createUser` (com password **temporaria** por defeito), `updateUser`,
  `deleteUser`, `setPassword`.
- **Realm role mappings:** `assignRealmRoles` / `assignRealmRolesByObject`,
  `removeRealmRolesByObject`, `getRealmRoleNames` (mappings **directos**),
  `getEffectiveRealmRoleNames` (composito/efectivo), `fetchRealmRoleOrNull`.

A distincao directos vs efectivos importa: so se pode **remover** um role
directamente mapeado, e o efectivo inclui o que e herdado por composites
(ex.: `fan` vem do default role `default-roles-tessera` no auto-registo),
usado para *display*.

#### Dependencia httpclient5

O `build.gradle.kts` depende de
`org.apache.httpcomponents.client5:httpclient5`. Com este no classpath, o
`RestTemplateBuilder` do Spring Boot passa a usar o
`HttpComponentsClientHttpRequestFactory` em vez do
`SimpleClientHttpRequestFactory` (HttpURLConnection) por defeito. E
necessario porque a remocao de realm role mappings e um pedido **DELETE
com corpo** — o factory por defeito nao consegue enviar corpo num DELETE.

### UserController — `/api/v1/users`

**Todos os endpoints exigem `platform-admin`** (`@PreAuthorize("hasRole('platform-admin')")`).

| Method | Path | Descricao |
|--------|------|-----------|
| GET | `/users` | Pesquisa (`?search=`, paginada por `?first=0&max=20`, `max` limitado a 100) |
| GET | `/users/{id}` | Detalhe (404 `UserNotFoundException` se inexistente) |
| POST | `/users` | Cria utilizador + atribui role `club-manager` ou `staff` |
| PUT | `/users/{id}` | Atualiza perfil e/ou reatribui role; force password reset opcional |
| DELETE | `/users/{id}` | Apaga (204) |

O `UserSummary` devolvido inclui os **realm roles efectivos** do
utilizador filtrados pelos roles de app (`platform-admin`, `club-manager`,
`staff`, `fan`).

**Criacao (`POST`):** valida `username` (3-60), `email`, `firstName`/
`lastName` (1-100), `password` (6-200) e `role` (regex `club-manager|staff`
— `platform-admin` e `fan` nao se atribuem por aqui). Resolve primeiro o
realm role (falha cedo com 400 se nao existir), cria o utilizador, e atribui
o role; se a atribuicao falhar, faz **rollback** apagando o utilizador
orfao. A password inicial e **temporaria** → o Keycloak forca a sua
alteracao no primeiro login. Devolve **201** com `Location`.

**Atualizacao (`PUT`):** faz merge dos campos `email`/`firstName`/
`lastName`/`enabled` (campos `null` mantem o valor atual). Se vier `role`,
reconcilia o role *gerivel* (`club-manager`/`staff`) — remove o antigo e
adiciona o novo — deixando `platform-admin`/`fan`/default roles intactos.
Se `forcePasswordReset = true`, acrescenta a required action
`UPDATE_PASSWORD` (forca nova password no proximo login).

### MembershipController — `/api/v1/clubs/{clubId}/members`

Gere quem e manager/staff de um clube especifico. As pertencas sao
representadas por **grupos Keycloak** sob
`/clubs/<clubId>/managers` e `/clubs/<clubId>/staff`, geridos pelo
`KeycloakGroupService` (`ensureClubGroups` / `deleteClubGroups`,
idempotentes). A presenca nestes grupos aparece na claim `groups` do JWT,
que o `ClubMembershipExtractor` traduz em decisoes de escopo no
`ClubAuthorizationService`.

**Escopo por clube:** todos os endpoints sao gated por
`@clubAuthz.canManageClub(authentication, #clubId)` — passa o
`platform-admin` (gere qualquer clube) ou o **manager do proprio clube**.
Os managers estao limitados ao role **STAFF** (verificacao server-side):
nao podem adicionar nem remover outros managers, nem atribuir
`platform-admin`. (Antes, estes endpoints eram exclusivos do
`platform-admin`.)

| Method | Path | Descricao |
|--------|------|-----------|
| GET | `/clubs/{clubId}/members` | Lista managers e staff do clube |
| POST | `/clubs/{clubId}/members` | Liga utilizador existente (`userId`) **ou** cria um novo inline; junta-o ao grupo (`role` MANAGER/STAFF, default STAFF) |
| DELETE | `/clubs/{clubId}/members/{userId}?role=...` | Remove do grupo |

**Criacao inline de staff (`POST`):** quando o pedido nao traz `userId`,
o `AddMemberRequest` aceita `username`/`email`/`firstName`/`lastName`/
`password` e aprovisiona um novo utilizador no Keycloak — com password
**temporaria**, o realm role `staff` e a pertenca ao grupo
`/clubs/<clubId>/staff`. Se a atribuicao de role falhar, o utilizador
orfao e apagado (rollback). Isto permite a um manager criar staff novo
sem aceder a lista global `/api/v1/users` (que permanece so
`platform-admin`).

### Configuracao

`KeycloakAdminProperties` (`@ConfigurationProperties("tessera.keycloak.admin")`),
lida via `application.yml` / variaveis de ambiente em `docker-compose.yml`:

| Propriedade | Env var | Default |
|-------------|---------|---------|
| `base-url` | `TESSERA_KEYCLOAK_ADMIN_BASE_URL` | `http://keycloak:8180` |
| `realm` | `TESSERA_KEYCLOAK_ADMIN_REALM` | `tessera` |
| `client-id` | `TESSERA_KEYCLOAK_ADMIN_CLIENT_ID` | `tessera-bff` |
| `client-secret` | `TESSERA_KEYCLOAK_ADMIN_CLIENT_SECRET` | _(override em producao)_ |

A service account do `tessera-bff` tem os roles `realm-management`
necessarios concedidos no realm export.

## Migracoes Flyway

| Versao | Ficheiro | Conteudo |
|--------|----------|----------|
| V1 | `V1__init.sql` | (heranca do monolito) |
| V2 | `V2__club_and_venue.sql` | tabelas `club` e `venue` |
| V3 | `V3__team.sql` | tabela `team` |
| V4 | `V4__player.sql` | tabela `player` (com constraints) |
| V5 | `V5__player_nationality_varchar.sql` | corrige tipo `nationality` |
| V6 | `V6__match.sql` | tabela `match` |
| V7 | `V7__match_sheet_lineup.sql` | `match_sheet` + `lineup_entry` |
| V8 | `V8__occurrence.sql` | tabela `occurrence` |
| V9 | `V9__match_status_cancelled.sql` | adiciona `CANCELLED` aos status validos |
| V10 | `V10__occurrence_foul.sql` | adiciona `FOUL` aos tipos de occurrence |

## Tratamento de erros

`GlobalExceptionHandler.kt` mapeia excecoes de dominio para Problem
Details:

| Excecao | Status | Type |
|---------|--------|------|
| `ClubNotFoundException`, `VenueNotFoundException`, `TeamNotFoundException`, `PlayerNotFoundException`, `MatchNotFoundException`, `LineupEntryNotFoundException`, `OccurrenceNotFoundException`, `UserNotFoundException` | 404 | `.../errors/not-found` |
| `*NameConflictException`, `*CategoryConflictException`, `*ShirtConflictException`, `InvalidMatchTransitionException`, `LineupConflictException`, `LineupRoleLimitException`, `TooManySubstitutionsException`, `PlayerSentOffException`, `SheetLockedException`, `SheetNotEditableException` | 409 | `.../errors/conflict` |
| `MethodArgumentNotValidException` (bean validation) | 400 | `.../errors/validation` |
| `HttpMessageNotReadableException` (JSON malformado) | 400 | `.../errors/malformed-json` |
| `AccessDeniedException` | 403 | `.../errors/forbidden` |
| `AuthenticationException` | 401 | `.../errors/unauthorized` |
| Outras | 500 | `.../errors/internal` |

## Testes

Os smoke tests vivem em `docs/http-tests/`:

- `01-clubs.http` (CRUD completo)
- `02-venues.http`
- `03-teams.http`
- `04-players.http`
- `05-matches.http` (com transicoes de estado)
- `06-match-sheets.http` (lineup + occurrences + lock/unlock)
- `99-rbac-checks.http` (negativos: 401, 403)

Total: aprox. **56 asserts** automaticos. Executaveis no IntelliJ
(HTTP Client) ou VS Code (REST Client).

## Decisoes de design

### Soft delete explicito no repositorio

Em vez de usar `@SQLDelete` + `@Where` do Hibernate (que filtra
silenciosamente), cada query JPA decide explicitamente se inclui
ou exclui linhas marcadas como apagadas. Beneficio: comportamento
previsivel; admin pode chegar a linhas apagadas se for preciso.

### Sem `@ManyToOne` para FKs simples

Os jogadores guardam `teamId: Long` em vez de `team: Team`. Evita
problemas de lazy loading e mantem entidades simples. Quando ha
necessidade de validar a existencia, faz-se via repository injetado
(ex: `TeamService` injeta `ClubRepository` para verificar o pai).

### Queries split-by-shape em vez de optionals

JPQL com `(:param IS NULL OR ...)` falha no Postgres porque os
parametros nullable sao inferidos como `bytea`. Solucao: dois metodos
separados no repositorio, e o service decide qual chamar.
Exemplo: `findAllActive` vs `findActiveByNameLike`.

### Specifications para Match

O endpoint `GET /matches` aceita ate 4 filtros opcionais
independentes (`from`, `to`, `status`, `clubId`). Usar
`JpaSpecificationExecutor` permite compor predicados dinamicamente
sem proliferar metodos no repositorio. O filtro `clubId` usa
subquery JPA Criteria para abranger home + away.
