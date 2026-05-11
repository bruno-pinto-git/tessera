# match-service

O microsservico responsavel pela atividade desportiva: clubes, equipas,
jogadores, jogos e fichas tecnicas. E o servico de dominio mais rico
do Tessera.

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
| POST | `/clubs` | admin | Cria |
| PATCH | `/clubs/{id}` | admin | Atualizacao parcial |
| DELETE | `/clubs/{id}` | admin | Soft delete |

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
| POST | `/venues` | admin |
| PATCH | `/venues/{id}` | admin |
| DELETE | `/venues/{id}` | admin |

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
| POST | `/clubs/{clubId}/teams` | admin |
| PATCH | `/teams/{id}` | admin |
| DELETE | `/teams/{id}` | admin |

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
| POST | `/teams/{teamId}/players` | admin | |
| PATCH | `/players/{id}` | admin | |
| DELETE | `/players/{id}` | admin | |

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
| POST | `/matches` | admin | Sempre criado em SCHEDULED |
| PATCH | `/matches/{id}` | admin / staff | Status transitions e scores |
| DELETE | `/matches/{id}` | admin | Soft delete |

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
| POST | `/matches/{id}/sheet/lineup` | staff / admin |
| PATCH | `/matches/{id}/sheet/lineup/{playerId}` | staff / admin |
| DELETE | `/matches/{id}/sheet/lineup/{playerId}` | staff / admin |
| POST | `/matches/{id}/sheet/occurrences` | staff / admin |
| DELETE | `/matches/{id}/sheet/occurrences/{occId}` | staff / admin |
| POST | `/matches/{id}/sheet/lock` | staff / admin |
| POST | `/matches/{id}/sheet/unlock` | **admin** |

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
| `ClubNotFoundException`, `VenueNotFoundException`, `TeamNotFoundException`, `PlayerNotFoundException`, `MatchNotFoundException`, `LineupEntryNotFoundException`, `OccurrenceNotFoundException` | 404 | `.../errors/not-found` |
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
