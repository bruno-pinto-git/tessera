# Tessera Frontend

React 19 + TypeScript + Vite 8 SPA for the Tessera platform.

## Stack

- **Build**: Vite 8 + `@vitejs/plugin-react`
- **Styling**: Tailwind CSS v4 (CSS-first config), shadcn/ui primitives,
  lucide-react icons
- **Routing**: react-router-dom v7
- **Auth**: keycloak-js (JWT pass-through to the BFF)
- **API**: typed wrapper over `fetch` in `src/api/client.ts`; types
  generated from the backend OpenAPI 3.1 spec

## Getting started

```bash
# from frontend/
npm install
npm run codegen:api      # generate types from ../docs/api/openapi.yaml
npm run dev              # http://localhost:5173
```

The dev server proxies `/api/**` to `VITE_API_PROXY_TARGET`
(default `http://localhost:8080` — the BFF). Adjust in `.env.development`
if you need to point elsewhere.

## Scripts

| Command                | What it does                                                       |
| ---------------------- | ------------------------------------------------------------------ |
| `npm run dev`          | Vite dev server with HMR                                           |
| `npm run build`        | TS check + production bundle into `dist/`                          |
| `npm run preview`      | Serve the built bundle for sanity checks                           |
| `npm run lint`         | ESLint                                                             |
| `npm run format`       | Prettier write                                                     |
| `npm run format:check` | Prettier check (CI-friendly)                                       |
| `npm run codegen:api`  | Regenerate `src/api/schema.gen.ts` from `../docs/api/openapi.yaml` |

## Project layout

```
src/
├── api/                  # API layer (typed)
│   ├── http.ts           # low-level fetch + JWT
│   ├── client.ts         # typed apiGet/apiPost/...
│   ├── schema.gen.ts     # GENERATED — do not edit
│   └── ticketApi.ts      # ticket-service endpoints
├── auth/                 # Keycloak wiring
│   ├── keycloak.ts
│   ├── AuthProvider.tsx
│   ├── AuthContext.ts
│   ├── useAuth.ts
│   └── ProtectedRoute.tsx
├── components/
│   ├── Layout.tsx        # app shell (header + main + footer)
│   └── ui/               # shadcn primitives (button, card, input, ...)
├── config/
│   └── env.ts            # required env-var helpers
├── features/             # feature folders (each with pages/, components/, hooks/)
│   ├── events/
│   ├── tickets/
│   └── validation/
├── lib/
│   └── utils.ts          # `cn()` helper for Tailwind class composition
├── pages/                # cross-feature pages (Home, 404, 403)
├── routes/
│   └── AppRoutes.tsx
├── App.tsx
├── main.tsx
└── index.css             # Tailwind v4 + design tokens
```

## Design system

The visual identity, design tokens, and component library are documented
in **[DESIGN.md](./DESIGN.md)**. Read that file before designing new
screens — Claude Design should ingest it during onboarding.

## Adding shadcn components

```bash
npx shadcn@latest add dialog form select tabs
```

Components land in `src/components/ui/` and pick up our tokens from
`index.css` automatically.

## Designing with Claude Design

1. Run `npm install` and `npm run dev` so the project compiles
2. Open [claude.ai/design](https://claude.ai/design)
3. Connect / upload this `frontend/` folder so it reads:
   - `DESIGN.md` (the source of truth for tokens + patterns)
   - `src/components/ui/` (the existing primitives)
   - `../docs/api/openapi.yaml` (the API contract)
4. Ask it to design a page (e.g. "écran de listagem dos meus bilhetes,
   com QR grande para os PAID"). Iterate.
5. Export the handoff bundle and bring it back to Claude Code (in the
   repo) to implement.

## Env vars

| Var                       | Example                                                                 | Used by                     |
| ------------------------- | ----------------------------------------------------------------------- | --------------------------- |
| `VITE_KEYCLOAK_URL`       | `http://localhost:8000` (via NGINX) or `http://localhost:8180` (direct) | `auth/keycloak.ts`          |
| `VITE_KEYCLOAK_REALM`     | `tessera`                                                               | `auth/keycloak.ts`          |
| `VITE_KEYCLOAK_CLIENT_ID` | `tessera-web`                                                           | `auth/keycloak.ts`          |
| `VITE_API_PROXY_TARGET`   | `http://localhost:8080`                                                 | `vite.config.ts` (dev only) |

See `.env.example` for the full list.
