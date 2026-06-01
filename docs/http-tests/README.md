# HTTP test suite

Smoke / regression tests for the Tessera APIs, written as `.http` files.
Executable directly in IntelliJ IDEA and VS Code (REST Client extension).

## Layout

```
http-tests/
├── http-client.env.json         # Environments (local-direct, local-via-nginx)
├── 00-auth.http                 # Get JWTs and store as global vars
├── 01-clubs.http                # Club CRUD
├── 02-venues.http               # Venue CRUD
├── 03-teams.http                # Team CRUD
├── 04-players.http              # Player CRUD
├── 05-matches.http              # Match CRUD + state transitions
├── 06-match-sheets.http         # Lineup, occurrences, lock/unlock
├── 07-sheet-rules.http          # Match-sheet business rules (roster/sub/red-card limits)
├── 08-statistics.http           # statistics-service read-side (match-sheet history + sales)
├── 09-tickets.http              # ticket-service end-to-end (event/buy/pay/validate)
├── 10-bff-passthrough.http      # BFF forwards auth + downstream status codes
└── 99-rbac-checks.http          # Negative auth tests (401, 403)
```

These `.http` files are **exploration / smoke tests only**. The
authoritative backend tests are the JUnit suites living in each
service's `src/test`.

## How to use

### IntelliJ IDEA

1. Open any `.http` file.
2. Top-right environment dropdown → choose `local-direct` (hits services
   on their own ports) or `local-via-nginx` (goes through the reverse
   proxy on `:8000`).
3. Click ▶ next to a request.
4. Run files in order: `00-auth.http` first (it logs in the three seed
   users — `admin` (platform-admin), `staff` (staff) and `adepto` (fan)
   — and sets `adminToken`, `staffToken`, `adeptoToken` as **global**
   variables that the other files read). A fourth seed user exists,
   `gestor` (club-manager), but the current smoke suite does not log it in.

### VS Code (REST Client extension)

1. Install the [REST Client](https://marketplace.visualstudio.com/items?itemName=humao.rest-client) extension.
2. Open a `.http` file → click `Send Request` above each block.
3. The extension also reads `http-client.env.json`; switch envs from
   the bottom-right status bar.

## Conventions

- Each file is independent except for tokens (set in `00-auth.http`).
- Requests that **create** something save the new id into a request-scoped
  variable using `> {% client.global.set("clubId", response.body.id); %}`
  so subsequent requests can reference it.
- Negative cases live in `99-rbac-checks.http` and assert non-2xx status
  codes via `client.test(...)`.

## Variables

- **Environment vars** (per-env, in `http-client.env.json`):
  `matchServiceUrl`, `ticketServiceUrl`, `statisticsServiceUrl`,
  `bffUrl`, `keycloakUrl`.
- **Global vars** (set at runtime by tests):
  `adminToken`, `staffToken`, `adeptoToken`,
  `clubId`, `teamId`, `playerId`, `matchId`, `venueId`,
  `ticketEventId`, `ticketId`, `ticketCode`, etc.

## Re-running everything

To reset the database and re-run the full suite:

```powershell
.\scripts\run\reset.ps1   # full reset (drops volumes)
# then in IntelliJ: open 00-auth.http and run all blocks top-to-bottom
```
