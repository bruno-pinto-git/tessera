# ticket-service

Servico responsavel pelos bilhetes digitais: catalogo de eventos,
compra, pagamento, listagem por utilizador e por jogo, e validacao no
portao via QR code.

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
| POST | `/api/v1/events` | admin |

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
| POST | `/api/v1/tickets` | authenticated | Cria bilhete em PENDING para o utilizador autenticado |
| GET | `/api/v1/tickets/mine` | authenticated | Lista os bilhetes do `sub` autenticado |
| GET | `/api/v1/tickets?eventId=` | staff, admin | Lista por evento |
| GET | `/api/v1/tickets/{id}` | owner OR staff/admin | Detalhe |
| POST | `/api/v1/tickets/{id}/pay` | owner OR staff/admin | PENDING → PAID |
| POST | `/api/v1/tickets/validate` | staff, admin | PAID → VALIDATED, scan no portao |

## QR code

A geracao do QR e responsabilidade do **cliente** (SPA / Android),
nao do backend. Mantemos o backend simples: cada bilhete tem um `code`
do tipo UUID v4 gerado por defaut na coluna (`gen_random_uuid()`), com
constraint `UNIQUE`. O cliente renderiza este UUID como QR (a SPA usa
o pacote `qrcode.react`; o Android usa ZXing).

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
{ "paymentMethod": "MBWAY", "mbwayReference": "REF-1234" }
```

Metodos permitidos: `MBWAY`, `CARD`, `CASH`. `mbwayReference` e
opcional (so faz sentido com MBWAY).

A integracao real com a passarela de pagamentos esta fora do scope do
projeto academico — o endpoint trata o request como confirmacao
**ja recebida** da passarela (o frontend chama-o no callback de
sucesso). Para extender: adicionar um servico externo (Stripe / MB WAY
SDK) e mover este endpoint para webhook.

Apos o commit, o evento `ticket.ticket.paid` e publicado e o
`statistics-service` insere uma linha em `ticket_sale`.

## Autorizacao

- `GET /api/v1/events/**` — publico
- Tudo o resto exige JWT valido
- `@PreAuthorize` por endpoint:
  - `isAuthenticated()` para criar bilhete / listar os meus / consultar
  - Owner OR staff/admin para pagar / consultar bilhete alheio
  - `hasAnyRole('staff','admin')` para validar / listar por evento
  - `hasRole('admin')` para criar eventos

## Tabelas

### `event` (V1)

`id, match_id, name, price_normal, price_supporter, status, created_at`

Constraints: precos ≥ 0; status ∈ {DRAFT, PUBLISHED, SALES_CLOSED, CANCELLED}.

### `ticket` (V1 + V2)

`id, event_id, code (UUID UNIQUE), price, status, payment_method,
mbway_reference, owner_sub, created_at, payment_date, validation_date,
validator_sub`

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

## Trabalho futuro

- [ ] Integracao real com passarela de pagamentos (Stripe / MB WAY SDK +
      webhook)
- [ ] Endpoint para transitar `event.status` (PUBLISHED → SALES_CLOSED)
- [ ] Limite por evento (capacidade do estadio) com row-level locking
- [ ] Refund flow (`VALIDATED → REFUNDED` se autorizado pelo admin)
- [ ] Bilhetes nominativos (foto / nome do titular para zonas sensiveis)
