# Tessera

**Plataforma digital de bilheteira e ficha técnica para clubes de futebol de divisões inferiores.**

Tessera permite a clubes amadores/semiprofissionais venderem bilhetes digitais (com QR validável à entrada do estádio) e gerirem a sua atividade desportiva — clubes, equipas, jogadores, jogos e fichas técnicas — a partir de uma única plataforma. Adeptos compram online; staff valida à porta; gestores administram o seu clube; administradores gerem o catálogo da plataforma.

> Projeto e Seminário · Licenciatura em Engenharia Informática e de Computadores · ISEL · 2025/2026

---

## Funcionalidades

Os acessos são determinados por papel (role), com autorização *scoped* por clube quando aplicável:

| Papel | O que pode fazer |
|-------|------------------|
| **Adepto** (`fan`) | Consultar o calendário de jogos com bilheteira aberta, comprar bilhetes (MB WAY / cartão) e consultar os seus bilhetes (QR, estado, histórico) em "Os meus bilhetes". |
| **Gestor** (`club-manager`) | Área "O meu clube" scoped ao(s) seu(s) clube(s): gerir equipas e jogadores, **criar/editar jogos em casa**, **abrir bilheteira** desses jogos, e gerir os **membros** do clube (adicionar/criar staff). Não acede a dados de outros clubes. |
| **Staff** (`staff`) | Validar bilhetes ao portão (app Android) e preencher a ficha técnica; consulta **read-only** do seu clube (equipas, jogadores, jogos, membros). |
| **Admin** (`platform-admin`) | Gestão completa da plataforma: clubes, estádios, equipas, jogadores, jogos, utilizadores (criar/editar/ativar, atribuir a clubes) e bilheteira de qualquer jogo. |

**Destaques**
- Bilhetes digitais com **QR code**, ciclo `PENDING → PAID → VALIDATED`.
- Pagamentos **MB WAY** (fluxo assíncrono com webhook) e **cartão**.
- Validação ao portão na **app Android** (lê o QR).
- Autenticação **OAuth 2.0 / OpenID Connect** (Keycloak), incluindo **login com Google**.
- Estatísticas históricas de vendas e fichas técnicas (read-side via eventos RabbitMQ).

---

## Arquitetura

Arquitetura de microsserviços atrás de um reverse proxy NGINX, com SPA web, app Android e infraestrutura *off-the-shelf* (Keycloak, RabbitMQ, PostgreSQL).

```
   Browser (React SPA)            Android app
        │                              │
        │  (login direto :8180)        │
        ▼                              ▼
   ┌─────────┐  /api/*          ┌──────────────┐
   │  NGINX  │ ───────────────► │  BFF (:8080) │  valida JWT + reencaminha (pass-through)
   │ (:8000) │  /* (SPA)        └───┬─────┬────┬┘
   └─────────┘                      │     │    │
                       ┌────────────┘     │    └─────────────┐
                       ▼                  ▼                   ▼
              ticket-service       match-service       statistics-service
                 (:8081)              (:8082)               (:8083)
                    │                    │                     │
                db-tickets          db-matches            db-statistics
                                                          ▲
        ticket/match ── eventos (RabbitMQ) ───────────────┘

   Keycloak (:8180) — Identity Provider (realm "tessera", OIDC, Google IdP)
```

| Serviço | Porta | Responsabilidade |
|---------|-------|------------------|
| **nginx** | 8000 | Reverse proxy: `/api/*` → BFF, serve a SPA |
| **bff-service** | 8080 | Backend for Frontend — valida o JWT e reencaminha (pass-through) aos serviços internos |
| **ticket-service** | 8081 | Eventos (bilheteira), bilhetes, QR, pagamentos, validação |
| **match-service** | 8082 | Clubes, equipas, jogadores, estádios, jogos, fichas técnicas, gestão de utilizadores (Keycloak Admin API) |
| **statistics-service** | 8083 | Relatórios históricos (consumidor de eventos RabbitMQ) |
| **keycloak** | 8180 | Autenticação/autorização (OIDC) |
| **rabbitmq** | 5672 / 15672 | Barramento de eventos |
| db-tickets / db-matches / db-statistics | — | PostgreSQL (uma BD por serviço) |

Cada serviço é um **OAuth2 resource server** independente (defesa em profundidade): o BFF valida o token e reencaminha-o, e cada serviço revalida e aplica `@PreAuthorize`. Detalhes em [`docs/architecture.md`](docs/architecture.md) e [`docs/security.md`](docs/security.md).

---

## Stack tecnológica

| Componente | Tecnologia |
|------------|-----------|
| Backend | Kotlin · Spring Boot 3.4 · Gradle 8.14 · JDK 21 |
| Frontend Web | React · TypeScript · Vite · Tailwind CSS |
| App móvel | Android (Kotlin · Jetpack Compose) |
| Base de dados | PostgreSQL 16 · Flyway (migrações) |
| Autenticação | Keycloak 26 (OIDC) |
| Mensageria | RabbitMQ 3.13 |
| Reverse proxy | NGINX |
| Contentorização | Docker · Docker Compose |
| API | OpenAPI 3.1 (spec em [`docs/api/`](docs/api/)) |

---

## Arranque rápido

**Pré-requisitos:** JDK 21, Node.js 20+, Docker Desktop, Git. (Em Windows; os scripts são PowerShell.)

```bash
git clone https://github.com/bruno-pinto-git/tessera.git
cd tessera
```

```powershell
# Faz tudo: compila os JARs (Gradle), constrói as imagens Docker e arranca os containers.
.\scripts\run\start.ps1
```

> **Build dos JARs é obrigatório.** Os Dockerfiles do backend **não** compilam o
> código — apenas copiam um JAR já construído (`backend/<serviço>/build/libs/*.jar`).
> É por isso necessário compilar os JARs com o Gradle **antes** de construir as
> imagens Docker. O `start.ps1` trata disto automaticamente: corre o
> `scripts/build/buildAll.ps1` (que faz `gradlew bootJar` aos 4 serviços) e só
> depois `docker compose up -d --build`.

Se preferires fazer os passos à mão (ou só (re)compilar os JARs, ex.: em CI):

```powershell
.\scripts\build\buildAll.ps1      # compila os 4 JARs (ou buildBffService.ps1, buildMatchService.ps1, ... individualmente)
docker compose up -d --build      # constrói as imagens e arranca
```

> O primeiro arranque demora mais (download de imagens Docker).

Depois de arrancar:

| Recurso | URL |
|---------|-----|
| Aplicação Web (SPA) | http://localhost:8000 |
| Área de administração | http://localhost:8000/admin |
| API (via BFF) | http://localhost:8000/api/v1 |
| Keycloak | http://localhost:8180 |
| RabbitMQ Management | http://localhost:15672 (`tessera` / `tessera`) |

### Utilizadores de teste (dev)

| Username | Password | Papel |
|----------|----------|-------|
| `admin` | `admin` | platform-admin |
| `gestor` | `gestor` | club-manager |
| `staff` | `staff` | staff |
| `adepto` | `adepto` | fan |

> Credenciais **apenas para desenvolvimento** (importadas de `infra/keycloak/realm-export.json`). O Keycloak corre em `start-dev --import-realm` sem volume persistente — o realm é reimportado a cada arranque.

### Popular dados de exemplo

```powershell
.\scripts\seed\seed-clubs.ps1     # clubes
.\scripts\seed\seed-venues.ps1    # estádios
.\scripts\seed\seed-liga3.ps1     # clubes/equipas da Liga 3
```

### Comandos úteis

```powershell
.\scripts\run\stop.ps1     # parar tudo
.\scripts\run\reset.ps1    # reset completo (apaga dados)
docker compose ps          # estado dos containers
docker compose logs -f ticket-service
```

Guia completo (incluindo IntelliJ, smoke tests `.http` e resolução de problemas): [`docs/getting-started.md`](docs/getting-started.md).

---

## Estrutura do repositório

```
tessera/
├── android/             # App Android (validação de bilhetes, ficha técnica)
├── backend/
│   ├── bff-service/     # Backend for Frontend (:8080)
│   ├── ticket-service/  # Bilheteira, bilhetes, pagamentos (:8081)
│   ├── match-service/   # Clubes, jogos, fichas, utilizadores (:8082)
│   └── statistics-service/  # Relatórios (:8083)
├── frontend/            # React SPA (TypeScript + Vite)
├── mock-mbway/          # Mock do gateway MB WAY (dev)
├── infra/
│   ├── keycloak/        # realm-export.json
│   └── nginx/           # nginx.conf + Dockerfile
├── scripts/             # build / run / seed (PowerShell)
├── docs/                # Documentação + spec OpenAPI
└── docker-compose.yml   # Orquestração
```

---

## Documentação

| Documento | Conteúdo |
|-----------|----------|
| [architecture.md](docs/architecture.md) | Visão geral, microsserviços, decisões de arquitetura |
| [security.md](docs/security.md) | Autenticação, autorização scoped por clube, RBAC, JWT pass-through |
| [keycloak.md](docs/keycloak.md) | Realm, clients, roles, Google IdP, utilizadores |
| [bff.md](docs/bff.md) · [match-service.md](docs/match-service.md) · [ticket-service.md](docs/ticket-service.md) · [statistics-service.md](docs/statistics-service.md) | Detalhe por serviço |
| [events/async-contracts.md](docs/events/async-contracts.md) | Contratos dos eventos RabbitMQ |
| [nginx.md](docs/nginx.md) · [docker.md](docs/docker.md) · [scripts.md](docs/scripts.md) | Infraestrutura e scripts |
| [getting-started.md](docs/getting-started.md) | Setup local detalhado |
| [api/](docs/api/) | Especificação OpenAPI 3.1 |

---

## Contexto académico

Desenvolvido no âmbito da unidade curricular **Projeto e Seminário** da **Licenciatura em Engenharia Informática e de Computadores** do **Instituto Superior de Engenharia de Lisboa (ISEL)**, ano letivo **2025/2026**.
