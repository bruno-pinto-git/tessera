# ticket-service

Servico responsavel pelos bilhetes digitais: catalogo de eventos,
compra, pagamento (sincrono para CARD/CASH, assincrono via gateway MB WAY),
listagem por utilizador e por jogo, e validacao no portao via QR code.

- **Porta:** 8081
- **Base de dados:** `tessera_tickets` (PostgreSQL 16)
- **Stack:** Spring Boot 3.4.4, Kotlin 1.9.25, JPA, Flyway, Spring AMQP,
  OAuth2 Resource Server
- **Base path:** `/api/v1`

## Arquitetura

```
   Cliente (SPA / Android)
            │ JWT
            ▼
   ┌────────────────┐
   │   BFF (8080)   │ (forward Authorization)
   └────────┬───────┘
            ▼
   ┌────────────────┐     publishes              ┌─────────────────┐
   │ ticket-service │ ─────► tessera.events ───► │ statistics-svc  │
   │     (8081)     │   ticket.ticket.paid       │  (read-side)    │
   │                │   ticket.ticket.validated  └─────────────────┘
   └────────┬───────┘
            ▼
   ┌────────────────┐
   │ tessera_tickets│
   └────────────────┘
```

O servico publica dois eventos AMQP:

- `ticket.ticket.paid` — quando uma transicao `PENDING → PAID` faz commit.
- `ticket.ticket.validated` — quando uma transicao `PAID → VALIDATED`
  faz commit.

A publicacao usa `TransactionSynchronizationManager.afterCommit` para
que o broker so veja o evento depois da gravacao em base de dados, sem
fantasmas em caso de rollback.

## Recursos

### `event`

Bilhete-mae para uma ocasiao vendavel (tipicamente um jogo). Permite
configurar dois precos (normal e supporter) e ligar opcionalmente a um
`match_id` do match-service. Estados: `DRAFT`, `PUBLISHED`,
`SALES_CLOSED`, `CANCELLED`. Apenas eventos `PUBLISHED` aceitam a
compra de bilhetes.

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/events` | publico |
| GET | `/api/v1/events/{id}` | publico |
| POST | `/api/v1/events` | autenticado: admin (qualquer jogo) ou manager do clube da casa |

O `status` no POST e opcional e faz default a `PUBLISHED`, de modo a
que os bilhetes possam ser comprados imediatamente. No fluxo web, este
endpoint e chamado a partir do match-service ("Abrir bilheteira" num
jogo) para criar um `event` `PUBLISHED` ligado ao `matchId`.

#### Autorizacao da abertura de bilheteira (escopo por clube)

Abrir uma bilheteira deixou de ser exclusivo do `platform-admin`. O
`POST /api/v1/events` usa `@PreAuthorize("isAuthenticated()")` e depois
faz um check em codigo (`EventController.authorizeCreate`):

- **`platform-admin`** — pode abrir bilheteira para **qualquer** jogo
  (ou ate sem `matchId`).
- **`club-manager`** — so pode abrir para um jogo cujo **clube da casa**
  ele gere. Sem `matchId`, e recusado com 403.

O clube da casa do jogo e resolvido pelo `MatchLookupClient`, um cliente
read-only minimo que chama o match-service
(`GET /api/v1/matches/{id}` — endpoint publico, sem token forwarded) e
le o campo `homeClubId` da resposta. Os clubes que o utilizador gere sao
parseados do claim `groups` do JWT (`/clubs/<id>/managers`), espelhando o
`ClubMembershipExtractor` do match-service. Se `homeClubId` nao constar
dos clubes geridos, devolve **403**.

A URL base do match-service vem da config `tessera.match-service.base-url`
(env `MATCH_SERVICE_URL`, default `http://match-service:8082`), tambem
declarada no `docker-compose.yml` do ticket-service.

### `ticket`

Bilhete digital. Inclui `code` (UUID com unique constraint, gerado
server-side e renderizado pelo cliente como QR code), `owner_sub`
(JWT `sub` do comprador), `validator_sub` (JWT `sub` do staff que
validou). Lifecycle:

```
   PENDING ──pay──▶ PAID ──validate──▶ VALIDATED
```

| Method | Path | Role | Descricao |
|--------|------|------|-----------|
| POST | `/api/v1/tickets` | authenticated | Cria bilhete em PENDING para o utilizador autenticado (`{ eventId, supporter }`) |
| GET | `/api/v1/tickets/mine` | authenticated | Lista os bilhetes do `sub` autenticado |
| GET | `/api/v1/tickets?eventId=` | staff, admin | Lista por evento |
| GET | `/api/v1/tickets/{id}` | owner OR staff/admin | Detalhe |
| POST | `/api/v1/tickets/{id}/pay` | owner OR staff/admin | Inicia pagamento (PENDING → PAID, ou aguarda MB WAY) |
| POST | `/api/v1/tickets/validate` | staff, admin | PAID → VALIDATED, scan no portao (so Android) |
| POST | `/api/v1/webhooks/mbway` | publico (gateway) | Callback MB WAY: confirma/recusa pagamento |

Notas de autorizacao:

- O corpo de `POST /tickets` e `{ eventId, supporter }`; o preco e
  derivado server-side a partir do `event` (normal vs supporter), o
  cliente nao envia preco.
- `pay` e `/tickets/{id}` usam `isAuthenticated()` no `@PreAuthorize` e
  depois validam **owner OR staff/admin** em codigo (devolvem 403 caso
  contrario).
- `validate` e `?eventId=` usam `@PreAuthorize("hasAnyRole('staff','admin')")`.
  Repare que estas verificacoes usam os nomes de autoridade `staff` e
  `admin`; o role de plataforma **platform-admin nao e aceite** nestes
  endpoints (apenas `staff` ou `admin`).

## QR code

A geracao do QR e responsabilidade do **cliente** (SPA / Android),
nao do backend. Mantemos o backend simples: cada bilhete tem um `code`
do tipo UUID v4 gerado por defaut na coluna (`gen_random_uuid()`), com
constraint `UNIQUE`. O cliente renderiza este UUID como QR (a SPA usa
o pacote `qrcode.react`; o Android usa ZXing).

A validacao no portao acontece **apenas na app Android** (staff). A
pagina `/validate` da SPA web foi **removida** — a SPA so trata da
compra de bilhetes; o scan e a transicao para `VALIDATED` sao feitos
pela app Android com leitor ZXing. O endpoint `POST /tickets/validate`
exige `staff`/`admin` (ver nota de autorizacao acima).

Quando o staff faz scan, o leitor extrai a string UUID e envia para
`POST /api/v1/tickets/validate` com `{ code: "..." }`. O servico
verifica:

1. O `code` existe (caso contrario 404).
2. O bilhete esta em `PAID` (caso contrario 409).
3. Atomicamente: muda para `VALIDATED`, grava `validation_date`,
   grava `validator_sub` (do JWT do staff), e regista o evento
   `ticket.ticket.validated`.

Isto torna a validacao **idempotente para um bilhete ja validado**:
uma segunda tentativa devolve 409 e o staff sabe que esta a tentar
re-usar um bilhete.

## Pagamento

`POST /api/v1/tickets/{id}/pay` aceita:

```json
{ "paymentMethod": "MBWAY", "phoneNumber": "351912345678", "mbwayReference": "REF-1234" }
```

Metodos permitidos: `MBWAY`, `CARD`, `CASH` (case-insensitive). Ha dois
fluxos consoante o metodo:

- **CARD / CASH** — *sincrono*. O bilhete transita `PENDING → PAID`
  imediatamente, grava `payment_date` e publica `ticket.ticket.paid`
  no commit. Nao ha gateway externo no loop.
- **MBWAY** — *assincrono*. `phoneNumber` e **obrigatorio**. O servico
  chama o `MbwayGatewayClient` (protocolo SIBS; `mock-mbway` em dev,
  SIBS real em producao via `tessera.mbway.gateway-url`), que empurra
  uma notificacao para o telemovel do cliente. O bilhete fica `PENDING`
  com o `mbway_transaction_id` gravado. A transicao para `PAID`
  acontece mais tarde, no `MbwayWebhookService`, quando o gateway chama
  o webhook. `mbwayReference` e opcional e so faz sentido com MBWAY.

### Webhook MB WAY

`POST /api/v1/webhooks/mbway` e **publico** (o gateway chama
server-to-server, sem JWT). O `MbwayWebhookService` correlaciona pelo
`transactionID` e:

- `Success` → bilhete PENDING passa a `PAID` e publica
  `ticket.ticket.paid` (post-commit).
- `Declined` / `Expired` → no-op; o bilhete fica `PENDING` e o
  utilizador pode tentar de novo.
- Idempotente: se o bilhete ja estiver `PAID`/`VALIDATED`, o webhook
  e no-op (defende contra entregas duplicadas do gateway).

A SPA, depois de iniciar um pagamento MBWAY, faz polling a
`GET /tickets/{id}` ate o estado resolver (PAID ou timeout). CARD
resolve no proprio request. CASH esta **desativado na SPA** (so e usado
ao balcao por staff/admin); MBWAY e CARD sao as opcoes web.

Apos o commit (sincrono ou via webhook), o evento `ticket.ticket.paid`
e publicado e o `statistics-service` insere uma linha em `ticket_sale`.

## Autorizacao

- `GET /api/v1/events/**` — publico
- `POST /api/v1/webhooks/mbway` — publico (gateway server-to-server)
- Tudo o resto exige JWT valido
- `@PreAuthorize` por endpoint:
  - `isAuthenticated()` para criar bilhete / listar os meus / consultar /
    pagar (com verificacao owner OR staff/admin feita em codigo no `pay`
    e no `getOne`)
  - `hasAnyRole('staff','admin')` para validar / listar por evento
  - `isAuthenticated()` para abrir bilheteira (`POST /events`), com check
    em codigo: admin (qualquer jogo) ou manager do clube da casa do jogo
    (resolvido via `MatchLookupClient` — ver seccao `event`)

Os roles da realm sao `platform-admin`, `club-manager`, `staff`, `fan`
(mapeados para autoridades `ROLE_*`). O identificador de utilizador e
resolvido do JWT preferindo `sub`, com fallback para
`preferred_username` e `sid` (a realm export atual nem sempre emite
`sub`).

## Tabelas

### `event` (V1)

`id, match_id, name, price_normal, price_supporter, status, created_at`

Constraints: precos ≥ 0; status ∈ {DRAFT, PUBLISHED, SALES_CLOSED, CANCELLED}.

### `ticket` (V1 + V2)

`id, event_id, code (UUID UNIQUE), price, status, payment_method,
mbway_reference, mbway_transaction_id, owner_sub, created_at,
payment_date, validation_date, validator_sub`

`mbway_transaction_id` guarda o id devolvido pelo gateway MB WAY ao
iniciar um pagamento, para o webhook correlacionar o callback ao bilhete.

Constraints:

- `chk_ticket_price` — price ≥ 0
- `chk_ticket_status` — status ∈ {PENDING, PAID, VALIDATED}
- `chk_ticket_payment_method` — IN (MBWAY, CARD, CASH) ou NULL
- `chk_ticket_payment_coherent` — payment_date NOT NULL sse status IN (PAID, VALIDATED)
- `chk_ticket_validation_coherent` — validation_date NOT NULL sse status = VALIDATED
- `chk_ticket_validator_coherent` — validator_sub NOT NULL quando status = VALIDATED (excepto linhas legacy)

## Eventos publicados

Contratos em [events/async-contracts.md](events/async-contracts.md).

| Routing key | Acao |
|-------------|------|
| `ticket.ticket.paid` | Apos `pay()` commit. Payload tem `ticketId`, `eventId`, `matchId?`, `price`, `paymentMethod`, `paidAt`. |
| `ticket.ticket.validated` | Apos `validate()` commit. Payload tem `ticketId`, `matchId?`, `validatedAt`, `validatorSub`. |

A publicacao falha em best-effort: erros do RabbitMQ sao logged mas nao
revertem a transacao de negocio. O statistics-service tolera
out-of-order delivery (consumidor de `validated` que chegue antes do
`paid` e ignorado com warning).

## Migracoes Flyway

| Versao | Conteudo |
|--------|----------|
| V1 | `event` + `ticket` |
| V2 | `owner_sub`, `validator_sub`; drop coluna legacy `validator_id BIGINT` |
| V3 | `mbway_transaction_id` (correlacao do webhook MB WAY) |

## Trabalho futuro

- [x] ~~Integracao com passarela de pagamentos MB WAY (gateway + webhook)~~
      — concluido (protocolo SIBS; `mock-mbway` em dev). Falta apenas
      ligar ao SIBS real e validar a assinatura AES-GCM no webhook.
- [ ] Endpoint para transitar `event.status` (PUBLISHED → SALES_CLOSED)
- [ ] Limite por evento (capacidade do estadio) com row-level locking
- [ ] Refund flow (`VALIDATED → REFUNDED` se autorizado pelo admin)
- [ ] Bilhetes nominativos (foto / nome do titular para zonas sensiveis)
