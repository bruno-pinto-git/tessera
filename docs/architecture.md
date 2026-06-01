# Arquitetura do Sistema

## Visao Geral

O Tessera e uma plataforma digital de gestao de bilheteira e ficha tecnica para clubes de futebol de divisoes inferiores. O sistema segue uma arquitetura de microsservicos, composta por quatro servicos backend, uma Single Page Application (SPA) web, uma aplicacao movel Android, e servicos de infraestrutura (NGINX, Keycloak, RabbitMQ, PostgreSQL).

A SPA web inclui a area publica de catalogo/compra de bilhetes e uma area completa de administracao para o papel `platform-admin` (`/admin` com gestao de clubes, equipas, jogadores, estadios, jogos e utilizadores; o UI das fichas tecnicas esta planeado). O papel `club-manager` ("Gestor") tem uma area de gestao **scoped** que espelha a do admin mas limitada aos clubes que gere (`/club` lista os clubes geridos; `/club/:id` reutiliza a mesma `ClubDetailPage` do admin): gere equipas e jogadores, os jogos **em casa**, a bilheteira desses jogos e os membros (apenas staff, incluindo criar staff novo inline) — sem tocar noutros clubes nem no catalogo da plataforma. A validacao de bilhetes na porta do estadio passou a ser exclusivamente Android (a antiga pagina `/validate` da SPA foi removida).

## Diagrama de Arquitetura

```
                          ┌─────────────┐         ┌─────────────┐
                          │   Browser   │ ─JWT──>  │   Keycloak  │
                          │  (React SPA)│ <─────── │ (port 8180) │
                          └──────┬──────┘  login   └──────▲──────┘
                                 │                        │
                          ┌──────▼──────┐                 │ (rede docker:
                          │   NGINX     │                 │  validacao JWT
                          │  (port 8000)│                 │  + Admin API)
                          └────┬───┬────┘                 │
                               │   │                       │
              ┌────────────────┘   └──────────────┐        │
              │                                     │        │
     ┌────────▼───────┐                   ┌────────▼────────┐
     │   /*  (SPA)    │                   │  /api/* → BFF   │
     │  Static Files  │                   │  (port 8080)    │
     └────────────────┘                   └──┬───┬───┬──────┘
                                             │   │   │
                            ┌────────────────┘   │   └──────────────┐
                            │                     │                   │
                   ┌────────▼────────┐ ┌─────────▼────┐ ┌────────────▼───────┐
                   │ ticket-service  │ │ match-service│ │ statistics-service  │
                   │  (port 8081)    │ │ (port 8082)  │ │   (port 8083)      │
                   └────────┬────────┘ └──────┬───────┘ └─────────┬──────────┘
                            │                 │ ─────────────────┘ │
                   ┌────────▼────────┐ ┌──────▼───────┐ ┌──────────▼─────────┐
                   │  db-tickets     │ │  db-matches  │ │   db-statistics     │
                   │  (PostgreSQL)   │ │ (PostgreSQL) │ │   (PostgreSQL)      │
                   └─────────────────┘ └──────────────┘ └─────────────────────┘

NGINX so encaminha /api/* para o BFF e serve a SPA (try_files → index.html).
O Keycloak NAO e proxiado pelo NGINX: a SPA fala diretamente com :8180
(VITE_KEYCLOAK_URL) e os servicos backend usam http://keycloak:8180 na rede
Docker. (Proxiar /admin/ do Keycloak colidia com as rotas /admin/* da SPA.)
```

## Microsservicos

### BFF Service (Backend for Frontend) — Porta 8080

**Estado:** funcional (proxy completo para match-service; ticket-service legado).

O BFF e o ponto de entrada unico para todas as chamadas API dos clientes (SPA web e aplicacao Android). As suas responsabilidades incluem:

- **Validar tokens JWT** emitidos pelo Keycloak (resource server)
- **Encaminhar pedidos** aos microsservicos internos preservando o cabecalho `Authorization` (incluindo `/api/v1/users`, encaminhado para o match-service para gestao de utilizadores)
- **Agregar chamadas** a multiplos microsservicos numa unica resposta (a desenvolver)
- **Adaptar respostas** ao formato esperado por cada frontend
- Desacoplar os clientes da topologia interna dos servicos

O BFF nao possui base de dados propria. Utiliza `RestTemplate` para comunicar com os servicos internos. Detalhes em [bff.md](bff.md).

### Ticket Service — Porta 8081

**Estado:** **feature-complete** para o scope P3 (compra + QR + listagens
+ validacao + eventos AMQP).

Responsavel por toda a gestao de bilheteira:

- Criacao e consulta de eventos (jogos com bilheteira ativa)
- Compra de bilhetes digitais (`PENDING → PAID → VALIDATED`) com
  ownership por JWT `sub`
- Listagem dos bilhetes do utilizador autenticado (`/tickets/mine`) e
  por evento (staff/admin)
- Validacao de bilhetes por UUID (payload do QR code) com identificacao
  do staff validador
- Pagamentos: MB WAY, multibanco/cartao, dinheiro
- Publica eventos `ticket.ticket.paid` e `ticket.ticket.validated` em
  RabbitMQ para o statistics-service

Base de dados: `tessera_tickets`. Detalhes em
[ticket-service.md](ticket-service.md).

### Match Service — Porta 8082

**Estado:** **feature-complete**. Todos os recursos do dominio implementados.

Gere toda a informacao relacionada com a atividade desportiva:

- Gestao de clubes e equipas
- Gestao de jogadores e planteis
- Gestao de estadios (venues)
- Criacao e gestao de jogos (calendario, status state machine)
- Ficha tecnica dos jogos (lineup, golos, cartoes, substituicoes)
- Lock/unlock de fichas + auto-lock quando o jogo termina
- Gestao de utilizadores (pacote `com.tessera.match.iam`): o `KeycloakAdminClient` e o `UserController` (`/api/v1/users`) encapsulam a Admin REST API do Keycloak, usando a service account do client confidencial `tessera-bff`, para criar/editar/ativar-desativar/forcar reset de password/eliminar utilizadores e ler os seus papeis

Base de dados: `tessera_matches`. Detalhes em [match-service.md](match-service.md).

**Decisao de design:** Os clubes, equipas e jogadores vivem neste servico (em vez de um servico separado) porque sao constantemente referenciados pela ficha tecnica. Separar criaria dependencias excessivas entre servicos e pontos de falha durante os jogos.

### Statistics Service — Porta 8083

**Estado:** funcional. Fichas tecnicas via `match.sheet.closed`; vendas
via `ticket.ticket.paid` / `ticket.ticket.validated` desde a P3.

Servico read-side de relatorios historicos. Recebe eventos assincronos
via RabbitMQ:

- `match.sheet.closed` do match-service → snapshot da ficha tecnica
- `ticket.ticket.paid` / `ticket.ticket.validated` do ticket-service → vendas

Expoe endpoints de consulta:

- `GET /api/v1/stats/match-sheets` (com filtros `clubId`, `playerId`,
  `season`, `status`)
- `GET /api/v1/stats/match-sheets/{matchId}` (snapshot completo)
- `GET /api/v1/stats/sales/summary` (admin)
- `GET /api/v1/stats/sales/by-match/{matchId}` (admin)
- `GET /api/v1/stats/sales/range?from=...&to=...` (admin)

Base de dados: `tessera_statistics`. Detalhes em
[statistics-service.md](statistics-service.md).

## Stack Tecnologica

| Componente | Tecnologia | Versao |
|------------|-----------|--------|
| Backend | Spring Boot + Kotlin | 3.4.4 / 1.9.25 |
| Frontend Web | React + TypeScript | — |
| Aplicacao Movel | Android (Kotlin + Jetpack Compose) | — |
| Base de Dados | PostgreSQL | 16 (Alpine) |
| Migracoes BD | Flyway | — |
| Autenticacao | Keycloak | 26.0 |
| Event bus | RabbitMQ | 3.13 (management-alpine) |
| Reverse Proxy | NGINX | Alpine |
| Contentorizacao | Docker + Docker Compose | — |
| Build Tool | Gradle (Kotlin DSL) | 8.14.2 |
| JDK | Eclipse Temurin | 21 |

## Comunicacao entre Servicos

A comunicacao entre o BFF e os microsservicos internos e feita via HTTP/REST sincrono, utilizando `RestTemplate` do Spring. Cada servico e acessivel via DNS interno do Docker (ex: `http://ticket-service:8081`).

Os clientes (SPA e Android) nunca comunicam diretamente com os microsservicos. Todas as chamadas passam pelo NGINX e sao encaminhadas para o BFF.

```
Cliente → NGINX (:8000) → BFF (:8080) → Servico Interno (:808X) → PostgreSQL
```

## Autenticacao e Autorizacao

O sistema usa **OAuth 2.0 / OpenID Connect** com Keycloak como Identity Provider, seguindo o padrao **JWT pass-through**:

1. Cliente faz login diretamente no Keycloak (`:8180`, nao via NGINX) e recebe um JWT
2. Cliente envia JWT em cada pedido (`Authorization: Bearer ...`)
3. BFF valida o token (assinatura via JWKS) e encaminha o `Authorization` ao servico downstream
4. Servico downstream re-valida o JWT e aplica `@PreAuthorize` por papel

```
Browser ──JWT──> NGINX ──> BFF (valida JWT) ──Authorization fwd──> match-service (re-valida + check role)
```

**Quatro papeis** no realm `tessera`: `platform-admin`, `club-manager`, `staff`, `fan`. Cada endpoint declara o papel exigido.

Defesa em profundidade: mesmo que a rede interna do Docker fosse comprometida, todos os servicos ainda exigem JWT valido. Detalhes em [security.md](security.md).

## Isolamento de Dados

Cada microsservico possui a sua propria base de dados PostgreSQL, garantindo isolamento total:

- `tessera_tickets` — dados de eventos e bilhetes
- `tessera_matches` — dados de clubes, jogadores, jogos e fichas tecnicas
- `tessera_statistics` — dados de relatorios e estatisticas

As migracoes sao geridas pelo Flyway de forma independente em cada servico.

## Decisoes de Arquitetura e Justificacoes

### Porque microsservicos e nao monolito?

O projeto envolve dominios distintos (bilheteira, gestao desportiva, estatisticas) com requisitos diferentes de disponibilidade. Durante um jogo, a validacao de bilhetes e critica — se o servico de estatisticas falhar, nao deve afetar a entrada dos adeptos.

### Porque um BFF em vez de API Gateway?

O sistema tem dois clientes distintos (SPA web e Android) com necessidades diferentes. O BFF permite adaptar as respostas a cada cliente sem sobrecarregar os servicos internos com logica de apresentacao.

### Porque NGINX em vez de Spring Cloud Gateway?

A arquitetura ja inclui NGINX como reverse proxy (requisito do projeto). Adicionar um Spring Cloud Gateway seria duplicar funcionalidade. O NGINX e suficiente para routing por URL e mais leve em recursos.

### Porque clubes/jogadores dentro do match-service?

Clubes, equipas e jogadores sao consumidos quase exclusivamente pelo contexto de jogos e fichas tecnicas. Separa-los num servico independente criaria chamadas HTTP constantes entre servicos, latencia adicional e pontos de falha durante o preenchimento da ficha tecnica em tempo real.

## Estrutura do Repositorio

```
tessera/
├── android/                  # Aplicacao Android (Kotlin + Jetpack Compose)
├── backend/
│   ├── bff-service/          # Backend for Frontend
│   ├── ticket-service/       # Gestao de bilheteira
│   ├── match-service/        # Jogos, fichas tecnicas, clubes, jogadores
│   ├── statistics-service/   # Relatorios e estatisticas
│   └── settings.gradle.kts   # Multi-module Gradle (IntelliJ)
├── mock-mbway/               # Mock do gateway de pagamento MB WAY (dev)
├── frontend/                 # React SPA (TypeScript + Vite)
├── infra/                    # Infra-as-config: imagens off-the-shelf
│   ├── keycloak/             # Configuracao do realm Keycloak
│   └── nginx/                # Configuracao e Dockerfile do NGINX
├── scripts/
│   ├── build/                # Scripts PowerShell de compilacao
│   └── run/                  # Scripts PowerShell de execucao/reset
├── db/                       # (reservado para scripts de BD)
├── docs/                     # Documentacao do projeto
│   ├── api/                  # Spec OpenAPI 3.1
│   └── http-tests/           # Smoke tests .http (IntelliJ / VS Code)
└── docker-compose.yml        # Orquestracao de todos os containers
```

## Indice da documentacao

| Documento | Conteudo |
|-----------|----------|
| [architecture.md](architecture.md) | Este documento — visao geral |
| [security.md](security.md) | Autenticacao, autorizacao, JWT pass-through, RBAC |
| [bff.md](bff.md) | Padrao proxy, controllers, configuracao |
| [match-service.md](match-service.md) | Recursos, endpoints, regras de negocio |
| [ticket-service.md](ticket-service.md) | Bilhetes digitais, QR, fluxo de pagamento, validacao |
| [statistics-service.md](statistics-service.md) | Read-side tables, consumers, endpoints de historico e vendas |
| [events/async-contracts.md](events/async-contracts.md) | Contratos dos eventos RabbitMQ |
| [keycloak.md](keycloak.md) | Realm, clients, roles, utilizadores |
| [nginx.md](nginx.md) | Configuracao do reverse proxy |
| [docker.md](docker.md) | Compose, networks, volumes |
| [scripts.md](scripts.md) | Build / start / reset scripts |
| [getting-started.md](getting-started.md) | Setup local rapido |
