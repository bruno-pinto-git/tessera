# Tessera — Design System

> **For Claude Design (and humans):** This document is the single source
> of truth for the frontend's visual identity, design tokens, component
> library, and page-level patterns. Read it during onboarding so the
> designs you produce match the codebase the engineering side will
> implement.

## Brand & Vibe

Tessera is the digital platform for lower-division Portuguese football
clubs to manage tickets, matches, and stats. The app is used by three
audiences:

- **Fans (`fan`)** — browse matches, buy tickets, scan their QR at the gate.
- **Staff (`staff`)** — validate tickets at the gate, fill match sheets.
- **Admin (`admin`)** — manage clubs, players, events, view stats.

**Visual tone**: modern, calm, _trustworthy_ — not corporate stiffness,
not over-the-top "matchday hype". Think Linear / Vercel / Stripe rather
than ESPN. Animations subtle, generous whitespace, clear hierarchy.

## Stack

- **React 19** + **TypeScript** + **Vite 8**
- **Tailwind CSS v4** (CSS-first config via `@tailwindcss/vite`)
- **shadcn/ui** primitives (copy-in components in `src/components/ui/`)
- **lucide-react** for icons
- **react-router-dom v7** for routing
- **keycloak-js** for auth (already wired up — JWT pass-through)

## Design Tokens

All tokens live in `src/index.css` as CSS custom properties exposed via
Tailwind v4's `@theme` block. **Do not reference raw hex values in
components** — always use Tailwind classes that resolve to these tokens
(`bg-primary`, `text-muted-foreground`, etc.).

### Colour roles (light theme)

| Token                              | OKLCH                                 | Use                       |
| ---------------------------------- | ------------------------------------- | ------------------------- |
| `--background` / `bg-background`   | `oklch(1 0 0)` (white)                | App canvas                |
| `--foreground` / `text-foreground` | `oklch(0.145 0 0)` (near-black)       | Default text              |
| `--primary` / `bg-primary`         | `oklch(0.45 0.13 152)` (forest green) | Brand actions, CTAs       |
| `--primary-foreground`             | `oklch(0.985 0 0)`                    | Text on `--primary`       |
| `--secondary`                      | `oklch(0.97 0 0)`                     | Subtle backgrounds        |
| `--muted-foreground`               | `oklch(0.556 0 0)`                    | Secondary text, hints     |
| `--accent`                         | `oklch(0.97 0 0)`                     | Hover states              |
| `--destructive`                    | `oklch(0.577 0.245 27.325)` (red)     | Errors, dangerous actions |
| `--border` / `border`              | `oklch(0.922 0 0)`                    | All borders, dividers     |
| `--ring`                           | green @ 40% alpha                     | Focus rings               |

### Semantic status colours (domain-specific)

Used by `<StatusBadge>` to render ticket / match status. Don't invent
new colours for status — use these.

| Token                | Tailwind class          | Meaning                          |
| -------------------- | ----------------------- | -------------------------------- |
| `--status-pending`   | `text-status-pending`   | Ticket created, not paid (amber) |
| `--status-paid`      | `text-status-paid`      | Ticket paid, ready to use (blue) |
| `--status-validated` | `text-status-validated` | Ticket scanned at gate (green)   |
| `--status-cancelled` | `text-status-cancelled` | Cancelled / refunded (red)       |

### Typography

- **Sans:** Inter, system fallback. All text uses this.
- **Mono:** JetBrains Mono. Used for ticket codes (UUIDs), API responses
  in dev pages, anything technical.
- Scale: rely on Tailwind defaults (`text-sm`, `text-base`, `text-lg`,
  `text-2xl`, `text-4xl`). Headings use `tracking-tight`.

### Radius

Base radius `--radius: 0.625rem`. Most components use `rounded-md`
(buttons, inputs) or `rounded-xl` (cards). Don't use sharp corners.

### Dark mode

`.dark` class on `<html>` flips tokens. Don't write `dark:` modifiers
in components — the tokens handle it. (Dark mode toggle UI is TODO.)

## Component Library

We ship `shadcn/ui new-york` style. All UI primitives live in
`src/components/ui/`. Already in the repo:

| Component                                                    | Path                  | Variants                                                                        |
| ------------------------------------------------------------ | --------------------- | ------------------------------------------------------------------------------- |
| `<Button>`                                                   | `ui/button.tsx`       | default, destructive, outline, secondary, ghost, link; sizes default/sm/lg/icon |
| `<Card>` (+ Header / Title / Description / Content / Footer) | `ui/card.tsx`         | one variant                                                                     |
| `<Input>`                                                    | `ui/input.tsx`        | one variant                                                                     |
| `<Label>`                                                    | `ui/label.tsx`        | one variant                                                                     |
| `<Badge>`                                                    | `ui/badge.tsx`        | default, secondary, destructive, outline, **pending/paid/validated/cancelled**  |
| `<StatusBadge>`                                              | `ui/status-badge.tsx` | maps domain status strings to the right Badge variant                           |
| `<Separator>`                                                | `ui/separator.tsx`    | horizontal / vertical                                                           |

If you need more (Dialog, Form, Select, Tabs, Toast, Table, Sheet, etc.),
add them via the shadcn CLI from inside `frontend/`:

```bash
npx shadcn@latest add dialog form select tabs
```

They'll auto-install into `src/components/ui/` and pick up our tokens
from `index.css`.

## Layout Conventions

- **Container**: `container mx-auto px-4`. Used by `<Layout>` for header,
  main, and footer.
- **Page wrapper**: feature pages should be inside the layout's main; no
  need to redeclare container.
- **Vertical rhythm**: use `space-y-{4|6|8|12}` between sections.
- **Cards**: prefer `<Card>` for grouped info. Avoid raw `<div class="border">`.
- **Forms**: stack with `space-y-4`, label above input, error below with
  `text-sm text-destructive`.

## Routes (current + planned)

| Path               | Role          | Status         | Component                                |
| ------------------ | ------------- | -------------- | ---------------------------------------- |
| `/`                | public        | ✅ done        | `HomePage`                               |
| `/events`          | public        | 🚧 stub        | `features/events/pages/EventsPage`       |
| `/events/:id`      | public        | ⏳ TODO        | not created yet                          |
| `/tickets/mine`    | authenticated | 🚧 stub        | `features/tickets/pages/MyTicketsPage`   |
| `/tickets/sandbox` | authenticated | ✅ dev sandbox | `features/tickets/pages/TicketsPage`     |
| `/validate`        | staff, admin  | 🚧 stub        | `features/validation/pages/ValidatePage` |
| `/admin`           | admin         | 🚧 inline      | inline in `AppRoutes`                    |
| `/unauthorized`    | —             | ✅ done        | `UnauthorizedPage`                       |
| `*`                | —             | ✅ done        | `NotFoundPage`                           |

## Pages We Need You (Claude Design) to Design

In priority order:

1. **`/events`** — Public catalog of upcoming events. Grid of cards,
   each card has: home club crest, "vs Away Club", date+time, venue,
   "Comprar bilhete" CTA. Filters for season + status.
2. **`/events/:id`** — Single event detail. Header with the match info,
   ticket pricing table (Normal / Sócio), big "Comprar" button. Below:
   already-paid tickets count (if admin viewing).
3. **`/tickets/mine`** — List of the caller's tickets, grouped by
   status. Paid tickets show a **large QR code** rendered from the
   `code` UUID (use `qrcode.react` library — please include it in the
   handoff). Validated tickets fade to muted so the user knows they're
   used.
4. **`/validate`** — Mobile-first. Big camera viewport that scans QR
   automatically (or manual UUID input as fallback). On scan: full-screen
   green ✓ for success, red ✗ for failure, large readable status text.
   This is used by staff under stress at the gate — keep it _ruthlessly
   simple_.
5. **Purchase modal/flow** — from `/events/:id` → choose price tier →
   payment method (MBWAY/Cartão/Dinheiro) → success state with the QR.

## Page Patterns

- **Page title**: `<h1 class="text-2xl font-bold tracking-tight">` or
  `text-4xl` for landing.
- **Page description**: `<p class="text-sm text-muted-foreground">`
  right under the title.
- **Empty states**: centre + muted text + a CTA back to where the user
  should go. See `NotFoundPage` / `UnauthorizedPage` for the pattern.
- **Loading**: `<Loader2 className="size-4 animate-spin" />` from
  lucide-react.
- **Error**: `<p role="alert" class="text-sm text-destructive">`.

## What's Already Wired

- **Auth**: `useAuth()` hook exposes `{ authenticated, username, roles,
hasRole, login, logout }`. Use this for conditional UI.
- **Protected routes**: wrap pages in `<ProtectedRoute roles={[...]}>`.
- **API client**: `src/api/client.ts` provides typed `apiGet`, `apiPost`,
  etc. They auto-attach the Keycloak JWT. Base path is `/api/v1`.
- **Feature API files**: `src/api/ticketApi.ts` already typed against
  the Spring backend. More features (events, matches, sales) to come.
- **OpenAPI codegen**: `npm run codegen:api` regenerates
  `src/api/schema.gen.ts` from `docs/api/openapi.yaml`. Run this any
  time the backend contract changes.

## What to Avoid

- ❌ Inline `style={{...}}` props — use Tailwind classes
- ❌ Raw hex values — use tokens
- ❌ Custom margins like `mt-7` — stick to Tailwind's spacing scale
- ❌ Bringing in MUI / Mantine / Chakra — we're shadcn-only
- ❌ Heavy animations — subtle `transition-colors` is enough
- ❌ Hardcoded English strings facing the user — Portuguese (pt-PT) for UI text

## Backend Contract Quick Reference

Full spec: `../docs/api/openapi.yaml`. Highlights:

- **Auth**: Bearer JWT from Keycloak. The `apiFetch` wrapper attaches
  it automatically.
- **Pagination envelope**: `{ content, page, size, totalElements, totalPages }`
- **Errors**: RFC 7807 Problem Details (`application/problem+json`):
  `{ type, title, status, detail, instance, errors? }`. The
  `ApiError` class in `client.ts` carries the parsed body.
- **Ticket lifecycle**: `PENDING → PAID → VALIDATED`. Use
  `<StatusBadge>` everywhere status shows.
