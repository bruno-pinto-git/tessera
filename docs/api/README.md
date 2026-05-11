# Tessera API Specification

OpenAPI 3.1 contract for the Tessera platform. Hand-written, multi-file,
authoritative — implementations and clients are derived from it.

## Layout

```
docs/api/
├── openapi.yaml                # entry point, $refs into the rest
├── components/
│   ├── securitySchemes.yaml    # bearer JWT + OIDC via Keycloak
│   ├── parameters/             # reusable query/path parameters
│   ├── responses/              # reusable error responses (RFC 7807)
│   └── schemas/                # reusable request/response shapes
├── paths/                      # one file per resource
├── .spectral.yaml              # linting rules
├── redocly.yaml                # bundling / Redoc HTML config
├── package.json                # tooling (Spectral, Redocly, openapi-generator)
└── README.md                   # you are here
```

## Setup

```bash
cd docs/api
npm install
```

## Common tasks

| Goal | Command |
|---|---|
| Lint the spec | `npm run lint` |
| Validate / parse | `npm run validate` |
| Bundle for BFF (single-file) | `npm run bundle:bff` |
| Build standalone Redoc HTML | `npm run build:redoc` |
| Run all the above | `npm run build` |
| Generate TypeScript client (web) | `npm run generate:web` |
| Generate Kotlin client (android) | `npm run generate:android` |

`npm run bundle:bff` writes the flattened spec to
`backend/bff-service/src/main/resources/static/openapi.yaml`. The BFF
serves Swagger UI from this file at <http://localhost:8080/api/docs>
(or <http://localhost:8000/api/docs> through NGINX). **Re-run the
bundle whenever you change a spec file**, or the BFF will serve a
stale version.

## Authoring conventions

- **One file per resource** under `paths/`. Each file's top-level keys
  are HTTP methods (`get`, `post`, ...).
- **Reuse aggressively** via `$ref` into `components/`. Schemas and
  responses defined once.
- **Every operation needs**: `operationId` (camelCase), `tags`,
  `summary`, `description`, complete `requestBody` schema (when
  applicable), at least one success response with example, and
  documented error responses.
- **Errors use Problem Details** (`application/problem+json`).
  Reference `components/responses/errors.yaml`.
- **Roles** declared via the `x-required-roles` extension on each
  operation: `[admin]`, `[staff, admin]`, `[fan, admin]`, or omit to
  mean "any authenticated user".
- **Date/time** values are ISO 8601 with timezone (`format: date-time`).
- **JSON casing** is camelCase.

## Spec ownership

| Resources | Author |
|---|---|
| `me`, `clubs`, `teams`, `players`, `venues`, `matches`, `matchSheets`, `stats` | Bruno |
| `tickets`, `events`, payment flow | Colleague |

Common pieces (security, errors, pagination) are shared and edited by
either author with PR review by the other.
