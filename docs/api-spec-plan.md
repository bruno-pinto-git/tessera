# Tessera — API Specification Plan (v1)

> **Status (beta) — what this plan got right, and what changed.**
> This was the original planning document. Most of it shipped; the notes
> below reconcile it with the current code and spec. The OpenAPI spec is
> live and multi-file under `docs/api/` (entry point `docs/api/openapi.yaml`),
> the BFF serves Swagger UI from the bundled copy, and the web SPA's types
> are generated from it via `npm run codegen:api` → `frontend/src/api/schema.gen.ts`.
>
> Implemented and in the spec:
> - Match domain: clubs, teams, players, venues, matches, match sheets
>   (lineup + occurrences + lock/unlock).
> - Ticketing: events and tickets (create → pay → list `/tickets/mine` →
>   gate `POST /tickets/validate`).
> - Statistics (read-side, RabbitMQ-fed): `/stats/match-sheets`,
>   `/stats/match-sheets/{matchId}`, `/stats/sales/summary`,
>   `/stats/sales/by-match/{matchId}`, `/stats/sales/range`.
> - Profile: `GET /me` (now also returns `clubMemberships`).
> - **User management (added after this plan):** `/users` — `GET` search,
>   `POST` create, `GET /users/{id}`, `DELETE /users/{id}` — `platform-admin`
>   only, wrapping Keycloak. Plus per-club membership at
>   `/clubs/{clubId}/members` and `/clubs/{clubId}/members/{userId}`.
>   All present in the OpenAPI spec.
>
> Deviations from this plan:
> - **Roles** are `platform-admin`, `club-manager`, `staff`, `fan` (this
>   doc's `🔴 admin` legend predates that). `x-required-roles` uses those
>   exact names.
> - **Match-sheet lineup** is per-player (`POST .../sheet/lineup`,
>   `PATCH/DELETE .../sheet/lineup/{playerId}`), not the bulk
>   `PUT .../sheet/lineup` upsert sketched in §6.7.
> - **Statistics surface** differs from §6.8: there is no
>   `/stats/players/{id}`, `/stats/teams/{id}` or `/stats/seasons/.../standings`
>   yet. The shipped stats are the match-sheet history and sales endpoints
>   listed above.
> - **Occurrence types** now also include `FOUL`.
> - **SPA `/validate` page was removed** — ticket validation is Android-only.
>   The `POST /tickets/validate` endpoint still exists and is exercised by
>   staff (see `docs/http-tests/09-tickets.http`).
>
> Gaps / not yet done: extended per-player and per-team statistics and
> season standings (§6.8) remain unimplemented.

## 1. Goal

Define a formal OpenAPI 3.1 contract for the Tessera REST API as a binding agreement between the three consumers: web SPA, android (Newland for staff, phone for fans), and the BFF. The spec is the source of truth — backend implementations and clients are derived from it.

## 2. Scope split between developers

| Owner | Domain |
|---|---|
| **Colleague** | `ticket-service` + `event` resources, payment flow (PENDING→PAID), QR validation endpoint, ticket retrieval |
| **You (Bruno)** | `match-service` (clubs, teams, players, matches, match sheets, venues), `statistics-service`, `GET /me` profile, the **Match ↔ Event glue** (BFF aggregation) |
| **Both, jointly** | OpenAPI conventions, error model, security scheme, RabbitMQ event contracts, phased rollout |

The OpenAPI spec is **one unified document** for the whole BFF surface. Each developer writes the path files for their resources; both review the other's PRs.

## 3. Architecture additions

### 3.1 RabbitMQ event bus (new)

statistics-service will keep its own denormalized read tables, fed via async events. This adds RabbitMQ to the stack.

**docker-compose:**
```yaml
rabbitmq:
  image: rabbitmq:3-management-alpine
  ports: ["5672:5672", "15672:15672"]
```

**Event contracts** (initial set — extend as needed):

| Event | Producer | Consumer(s) |
|---|---|---|
| `TicketPaid` | ticket-service | statistics-service |
| `TicketValidated` | ticket-service | statistics-service |
| `MatchSheetClosed` | match-service | statistics-service |
| `MatchFinished` | match-service | statistics-service |

Event payloads are JSON, versioned (`{ "version": 1, "occurredAt": "...", ... }`), and documented in `docs/events/` as a parallel contract file (OpenAPI doesn't cover async — we'll just use plain markdown with payload schemas).

### 3.2 Android: one app, role-aware

Single Android codebase deployed to both device types. After login, the app reads the Keycloak role and routes accordingly:

- **`staff` role** (Newland device): validate QR (existing `ValidateScreen`), match sheet entry
- **`fan` role** (phone): my tickets (with QR display), browse matches, ticket purchase entry point (calls colleague's payment flow)

The hardcoded LAN IP and missing auth in current android code become tracked debt — addressed before v1 ships, but not in this spec task.

## 4. Domain model

### 4.1 Match-service entities

```
Club { id, name, foundedYear?, crestUrl?, deletedAt? }
  └── Team (1..N) { id, clubId, category (SENIOR_M/SENIOR_F/SUB_19/...), deletedAt? }
        └── Player (0..N) { id, teamId, firstName, lastName, birthdate, nationality,
                            position (GK/DF/MF/FW), shirtNumber, photoUrl?, dominantFoot,
                            height, weight, status (ACTIVE/INJURED/SUSPENDED), deletedAt? }

Venue { id, name, capacity, address, deletedAt? }

Match { id, homeTeamId, awayTeamId, venueId, kickoffAt, status, homeScore?, awayScore?,
        refereeName?, deletedAt? }
  status ∈ { SCHEDULED, LIVE, FINISHED, POSTPONED, ABANDONED }

MatchSheet { id, matchId, locked, lockedAt? }
  └── LineupEntry { matchSheetId, teamId, playerId, shirtNumber,
                    role (STARTER/SUBSTITUTE) }
  └── Occurrence { id, matchSheetId, minute, type, teamId, playerId,
                   replacedPlayerId? }
       type ∈ { GOAL, OWN_GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION }
```

**Soft delete** on Club, Team, Player, Match, Venue (column `deleted_at`). Hard delete on Lineup/Occurrence (rebuilt freely while sheet unlocked).

### 4.2 Ticket-service entities (colleague's, referenced for context)

`Event` already has `match_id`. **Glue rule**: the BFF, when serving `GET /matches/{id}`, can optionally aggregate the linked Event (price, ticket availability) by calling ticket-service. Match-service itself does **not** know about events.

### 4.3 Statistics-service entities (read-side only)

Denormalized tables fed by RabbitMQ events. Examples:
- `match_stats { match_id, home_score, away_score, scorers JSONB, cards JSONB, ... }`
- `player_stats { player_id, season, appearances, goals, yellow_cards, red_cards, minutes_played }`
- `team_stats { team_id, season, played, won, drawn, lost, gf, ga }`
- `event_sales { event_id, sold, validated, revenue, capacity }`

## 5. API conventions

| Convention | Value |
|---|---|
| Base path | `/api/v1` (versioned from day one) |
| JSON casing | `camelCase` |
| Date/time | ISO 8601 with timezone (`2026-05-12T20:30:00Z`) |
| Locale | English (status enums, field names) — UI translates |
| Pagination | Offset (`?page=0&size=20&sort=field,asc`) |
| Pagination envelope | `{ content: [...], page, size, totalElements, totalPages }` |
| Errors | RFC 7807 Problem Details (`application/problem+json`) — `{ type, title, status, detail, instance, errors? }` |
| Security | OAuth 2.0 / OIDC bearer JWT from Keycloak. Documented as `securityScheme: oauth2`. Required role(s) per endpoint declared via custom `x-required-roles` extension. |
| OperationId | `camelCase` verb-noun: `listMatches`, `getMatchById`, `addOccurrence` |

## 6. Endpoint catalog (your scope)

Strawman — every entry needs to be filled out (request/response schema, errors, examples) when authoring the spec. `🟢 fan` `🟡 staff` `🔴 admin` `⚪ public` indicate required roles. **Note:** as shipped, `🔴 admin` maps to `platform-admin` (with `club-manager` also allowed on club-scoped writes via `@clubAuthz`); see the status note at the top of this file for the actual role names and the final stats/lineup surface.

### 6.1 Profile
| Method | Path | Roles | Notes |
|---|---|---|---|
| GET | `/me` | any | Echoes name, email, roles from JWT |

### 6.2 Clubs
| Method | Path | Roles |
|---|---|---|
| GET | `/clubs` | ⚪ |
| GET | `/clubs/{id}` | ⚪ |
| POST | `/clubs` | 🔴 |
| PATCH | `/clubs/{id}` | 🔴 |
| DELETE | `/clubs/{id}` | 🔴 (soft) |

### 6.3 Teams
| Method | Path | Roles |
|---|---|---|
| GET | `/clubs/{clubId}/teams` | ⚪ |
| GET | `/teams/{id}` | ⚪ |
| POST | `/clubs/{clubId}/teams` | 🔴 |
| PATCH | `/teams/{id}` | 🔴 |
| DELETE | `/teams/{id}` | 🔴 (soft) |

### 6.4 Players
| Method | Path | Roles |
|---|---|---|
| GET | `/teams/{teamId}/players` | ⚪ |
| GET | `/players/{id}` | ⚪ |
| POST | `/teams/{teamId}/players` | 🔴 |
| PATCH | `/players/{id}` | 🔴 |
| DELETE | `/players/{id}` | 🔴 (soft) |

### 6.5 Venues
| Method | Path | Roles |
|---|---|---|
| GET | `/venues` | ⚪ |
| GET | `/venues/{id}` | ⚪ |
| POST | `/venues` | 🔴 |
| PATCH | `/venues/{id}` | 🔴 |
| DELETE | `/venues/{id}` | 🔴 (soft) |

### 6.6 Matches
| Method | Path | Roles |
|---|---|---|
| GET | `/matches` | ⚪ — filters: `?from`, `?to`, `?clubId`, `?status` |
| GET | `/matches/{id}` | ⚪ — BFF aggregates linked Event (price, availability) |
| POST | `/matches` | 🔴 |
| PATCH | `/matches/{id}` | 🔴 (status transitions, score, refereeName) |
| DELETE | `/matches/{id}` | 🔴 (soft) |

### 6.7 Match sheets
| Method | Path | Roles |
|---|---|---|
| GET | `/matches/{id}/sheet` | ⚪ |
| PUT | `/matches/{id}/sheet/lineup` | 🟡🔴 — full upsert per team |
| POST | `/matches/{id}/sheet/occurrences` | 🟡🔴 — append occurrence |
| DELETE | `/matches/{id}/sheet/occurrences/{occId}` | 🟡🔴 — only while unlocked |
| POST | `/matches/{id}/sheet/lock` | 🟡🔴 — final action |
| POST | `/matches/{id}/sheet/unlock` | 🔴 — admin override |

### 6.8 Statistics
| Method | Path | Roles |
|---|---|---|
| GET | `/stats/matches/{id}` | ⚪ |
| GET | `/stats/players/{id}?season=YYYY-YY` | ⚪ |
| GET | `/stats/teams/{id}?season=YYYY-YY` | ⚪ |
| GET | `/stats/seasons/{season}/standings?clubId=` | ⚪ |
| GET | `/stats/events/{id}/sales` | 🔴 |

## 7. OpenAPI spec organization

```
docs/api/
├── openapi.yaml                       # entry point, $ref into the rest
├── components/
│   ├── securitySchemes.yaml           # oauth2 / bearer
│   ├── parameters/
│   │   ├── pagination.yaml            # page, size, sort
│   │   └── path.yaml                  # id, clubId, ...
│   ├── responses/
│   │   └── errors.yaml                # Problem responses (400/401/403/404/409/500)
│   └── schemas/
│       ├── problem.yaml               # RFC 7807
│       ├── pageEnvelope.yaml
│       ├── club.yaml, team.yaml, player.yaml, venue.yaml
│       ├── match.yaml, matchSheet.yaml, occurrence.yaml
│       ├── stat.yaml
│       └── ticket.yaml, event.yaml    # colleague writes
└── paths/
    ├── me.yaml
    ├── clubs.yaml, teams.yaml, players.yaml, venues.yaml
    ├── matches.yaml, matchSheets.yaml
    ├── stats.yaml
    └── tickets.yaml, events.yaml      # colleague writes

docs/events/
└── async-contracts.md                 # RabbitMQ event payloads
```

## 8. Tooling

### 8.1 Spec authoring & docs
- **Live Swagger UI in BFF** at `/api/docs` — uses `springdoc-openapi-starter-webmvc-ui` configured to serve our hand-written spec (not generated from code). Controllers point at the YAML.
- **Static Redoc/Redocly bundle** generated as part of the build for sharing without running BFF: `npx @redocly/cli build-docs docs/api/openapi.yaml -o docs/api/redoc.html`. Drop into `docs/api/`.

### 8.2 Linting

**Spectral** is to OpenAPI what ESLint is to TypeScript: a linter that enforces conventions. With a `.spectral.yaml` config we add rules like *"every endpoint must have a description"*, *"every response must declare `application/problem+json` for errors"*, *"operationIds must be camelCase"*, etc. Catches drift before merge.

Recommendation: **yes, set it up** with the Redocly preset + a few custom rules. Adds ~15 min to CI but pays off in spec quality. We can defer the strictness — start permissive, tighten over time.

### 8.3 Generated clients

Tools that read the OpenAPI YAML and **emit fully-typed client code**, so consumers don't hand-write fetch calls.

Concrete benefit, before:
```kotlin
// android — current code
val body = """{"code": "$code"}"""
val request = Request(POST, "$BASE_URL/api/tickets/validate")
    .header("Content-Type", "application/json")
    .body(body)
val response = client(request)
val status = response.status.code  // stringly-typed everything
```

After (with generated client):
```kotlin
val response = ticketsApi.validateTicket(ValidateTicketRequest(code))
//                                       ^^^^^^^^^^^^^^^^^^^^^^^ typed, autocompleted
```

Same on the web side — `apiFetch("/tickets", ...)` becomes `ticketsApi.create({ eventId: 1 })` with TS types.

Generated outputs:
- `frontend/src/api/generated/` — typescript-fetch from `openapi-generator-cli`. Wrapped by existing `apiFetch` helper to preserve auth refresh.
- `android/app/src/generated/` — kotlin client (Retrofit + Moshi).

Both via npm/Gradle scripts: `npm run generate:api` / `./gradlew generateApiClient`. Regenerate when spec changes.

Recommendation: **yes, both**. Especially valuable here since we're contract-first with three consumers.

### 8.4 No mock server

Skipped per decision. Frontend/android develop against the real BFF as it comes online.

## 9. Phased delivery plan

**Phase 0 — alignment session (~1 hour, both devs)**
- Walk through this document together, lock open items in §10
- Pin OpenAPI conventions: error model, pagination, security scheme, versioning
- Agree on RabbitMQ event payload shape

**Phase 1 — spec skeleton (you, ~half day)**
- Create `docs/api/` tree
- Write `openapi.yaml`, `securitySchemes`, `problem.yaml`, pagination, common error responses
- Wire **Spectral** + **Redocly** + **openapi-generator** into CI/scripts
- Stand up Swagger UI in BFF pointing at the YAML

**Phase 2 — your resources (you, ~2-3 days)**
Author one resource at a time, review-as-you-go: clubs → teams → players → venues → matches → match sheets → stats → me. Each PR includes the path file + schemas + examples + at least one happy path and one error.

**Phase 3 — colleague's resources (colleague, in parallel with Phase 2)**
- `tickets.yaml`, `events.yaml`, `payments.yaml` (whatever shape his payment flow needs)
- Same rigor: schemas, examples, error cases

**Phase 4 — RabbitMQ (both, ~1 day)**
- Add rabbitmq to `docker-compose.yml`
- Define event payloads in `docs/events/async-contracts.md`
- Wire publishers in ticket-service & match-service, consumer in statistics-service
- Implement statistics-service read tables + Flyway migrations

**Phase 5 — implementation against the spec**
- BFF gets routes for match-service & statistics-service (you)
- match-service & statistics-service get their controllers/repos (you)
- Generated clients land in frontend & android
- Real screens get wired

## 10. Open items needing alignment with colleague

These aren't decisions one developer can make alone — needs his input:

1. **Where do fans buy tickets?** Web only, android only, or both? Fans see their ticket QR on android. Buying could be:
   - **(a)** Web-only purchase → ticket appears on android. Simpler. Android = read-only ticket viewer + match sheet (for staff).
   - **(b)** Buy on android too (calls his payment flow). Requires fan-purchase screens on android.
   Default proposal: **(a)** for v1.

2. **Event creation flow.** Picked option (b): admin creates match in match-service, then separately creates event in ticket-service. Confirm this matches what he's building, and whether the BFF should have a "create-match-with-event" composite endpoint or two separate calls from the admin UI.

3. **Capacity.** Default value — what number? 1000? Should it live on `Event` or be derived from `Venue.capacity`? Suggestion: `Venue.capacity` is the physical limit, `Event.capacity` is the sellable limit (often less). Default `Event.capacity = Venue.capacity` at creation time.

4. **Event payload over RabbitMQ.** Final shape of `TicketPaid` / `TicketValidated`. Statistics-service needs at least: `eventId`, `matchId`, `ticketId`, `price`, `paidAt` / `validatedAt`. He picks the exact shape since he's the producer.

5. **Android dual deployment strategy.** Single APK with role-routing vs. two product flavors. Single APK is simpler; flavors give cleaner per-device builds. Whoever owns the Android client picks.

6. **Operational `tessera-bff` service account flow.** The realm has `tessera-bff` with `serviceAccountsEnabled: true`. If the BFF needs to call services on its own behalf (not user-on-behalf-of) for stats aggregation, confirm that flow.