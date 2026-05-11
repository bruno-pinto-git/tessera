# Async Event Contracts

Eventos publicados em RabbitMQ entre microsservicos do Tessera. Cada
evento e:

- Versionado via campo `version` (incrementa quando o shape muda)
- Timestamped via `occurredAt` (ISO 8601 com timezone)
- JSON puro, sem wrapper (o broker fica responsavel pelo routing key)

## Topologia

Exchange unico do tipo **topic**: `tessera.events`.

Convencao de routing keys: `{producer}.{aggregate}.{event-name}`, tudo
em minusculas com pontos.

| Routing key | Producer | Consumer(s) |
|-------------|----------|-------------|
| `match.sheet.closed` | match-service | statistics-service |
| `ticket.ticket.paid` | ticket-service | statistics-service |
| `ticket.ticket.validated` | ticket-service | statistics-service |

Cada consumer declara a sua propria queue durable, ligada ao exchange
com o routing key relevante. Mensagens persistentes; consumers fazem
ACK manual apos commit da transacao.

## Eventos

### `match.sheet.closed`

Emitido pelo **match-service** quando uma `MatchSheet` e fechada — quer
por accao manual (`POST /matches/{id}/sheet/lock`) quer por auto-lock
(quando o match transita para um estado terminal: `FINISHED`,
`ABANDONED`, `POSTPONED`, `CANCELLED`).

O payload contem **tudo** o que o statistics-service precisa para criar
o seu snapshot historico — match info + lineup + occurrences. Evita
multiplas chamadas e elimina race conditions.

```json
{
  "version": 1,
  "occurredAt": "2026-09-15T22:35:12.345Z",
  "matchId": 42,
  "season": "2026-27",
  "matchStatus": "FINISHED",
  "kickoffAt": "2026-09-15T20:30:00Z",
  "homeTeamId": 1,
  "awayTeamId": 2,
  "homeClubId": 10,
  "awayClubId": 20,
  "venueId": 5,
  "homeScore": 2,
  "awayScore": 1,
  "refereeName": "Maria Santos",
  "lineup": [
    {
      "playerId": 100,
      "teamId": 1,
      "shirtNumber": 9,
      "role": "STARTER"
    }
  ],
  "occurrences": [
    {
      "occurrenceId": 1001,
      "minute": 34,
      "type": "GOAL",
      "teamId": 1,
      "playerId": 100,
      "replacedPlayerId": null
    },
    {
      "occurrenceId": 1002,
      "minute": 60,
      "type": "SUBSTITUTION",
      "teamId": 1,
      "playerId": 101,
      "replacedPlayerId": 100
    }
  ]
}
```

**Notas:**

- `season` e derivada do `kickoffAt`: epoca corre de Julho a Junho
  (ex: jogos entre 1-Jul-2026 e 30-Jun-2027 → `"2026-27"`).
- `matchStatus` reflete o estado **no momento** do close. Se a sheet
  e reaberta (`unlock` por admin) e re-fechada, e emitido um novo
  evento com o estado actual.
- `homeClubId`/`awayClubId` sao derivados (`team.clubId`); incluidos
  para o statistics evitar joins cross-service.

### `ticket.ticket.paid` *(producer: ticket-service)*

Emitido apos a transicao `PENDING → PAID` fazer commit em base de
dados. `matchId` e nullable porque um `event` pode existir sem estar
ligado a um match especifico.

```json
{
  "version": 1,
  "occurredAt": "2026-09-15T18:42:00.000Z",
  "ticketId": 12345,
  "eventId": 7,
  "matchId": 42,
  "price": "10.00",
  "paymentMethod": "MBWAY",
  "paidAt": "2026-09-15T18:41:55.000Z"
}
```

### `ticket.ticket.validated` *(producer: ticket-service)*

Emitido apos a transicao `PAID → VALIDATED` fazer commit. `validatorSub`
e o `sub` (UUID) do JWT do staff/admin que fez o scan no portao —
nao um id numerico, porque o ticket-service nao tem tabela de
utilizadores; a fonte de verdade e o Keycloak.

```json
{
  "version": 1,
  "occurredAt": "2026-09-15T20:25:30.000Z",
  "ticketId": 12345,
  "matchId": 42,
  "validatedAt": "2026-09-15T20:25:29.000Z",
  "validatorSub": "1f8f29c4-8a3b-4c11-8d92-2db1bdf02a07"
}
```

## Idempotencia

Consumers devem ser idempotentes. Estrategia:

1. Para `match.sheet.closed`: a row da `match_summary` e identificada
   por `match_id`; usar UPSERT (na pratica, `DELETE WHERE match_id=...`
   + `INSERT`) garante que reprocessar uma mensagem produz o mesmo
   estado final.
2. Para `ticket.ticket.paid` e `ticket.ticket.validated`: identificados
   por `ticket_id`; mesma estrategia (UPSERT).

## Versionamento

Quando uma alteracao quebra compatibilidade:

1. Incrementar `version`
2. Manter consumers a aceitar as duas versoes durante o periodo de
   transicao
3. Documentar deprecation aqui

## Trabalho futuro

- [ ] Schema registry (ex: JSON Schema) para validar payloads em build-time
- [ ] Dead-letter queue para mensagens nao processaveis
- [ ] Retry com backoff exponencial
- [ ] Tracing (cabecalhos `traceId`/`spanId` propagados nos eventos)
