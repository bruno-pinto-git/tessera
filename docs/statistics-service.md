# statistics-service

Servico read-side de relatorios historicos. Recebe eventos assincronos
via RabbitMQ dos outros servicos, popula tabelas denormalizadas, e
expoe endpoints de consulta.

> Desde a P3 (tickets) os producers `ticket.ticket.paid` e
> `ticket.ticket.validated` ja existem no ticket-service — os
> endpoints de vendas devolvem dados reais.

- **Porta:** 8083
- **Base de dados:** `tessera_statistics` (PostgreSQL 16)
- **Stack:** Spring Boot 3.4.4, Kotlin 1.9.25, JPA, Flyway, Spring AMQP,
  OAuth2 Resource Server
- **Base path:** `/api/v1/stats`

## Arquitetura

```
match-service          ticket-service
     │                       │
     │ match.sheet.closed    │ ticket.ticket.paid
     │                       │ ticket.ticket.validated
     ▼                       ▼
       ┌───────────────────┐
       │   tessera.events  │   (RabbitMQ topic exchange)
       └─────────┬─────────┘
                 │
                 ▼
       statistics-service
        ├─ stats.match-sheet-closed   ──▶ MatchSummary + LineupSnapshot + OccurrenceSnapshot
        ├─ stats.ticket-paid          ──▶ TicketSale (insert)
        └─ stats.ticket-validated     ──▶ TicketSale.validatedAt (update)
```

Os consumers sao idempotentes: re-receber um evento para o mesmo
agregado produz o mesmo estado final. Detalhes do contrato em
[events/async-contracts.md](events/async-contracts.md).

## Tabelas read-side

### `match_summary`

Linha por match cuja sheet foi fechada. Reescrita inteiramente quando
chega novo `match.sheet.closed` para o mesmo `match_id`.

Colunas: `match_id (PK)`, `season`, `match_status`, `kickoff_at`,
`home_team_id`, `away_team_id`, `home_club_id`, `away_club_id`,
`venue_id`, `home_score`, `away_score`, `referee_name`, `snapshot_at`.

### `lineup_snapshot`

Linha por jogador no lineup da sheet fechada. Composite PK
`(match_id, player_id)`. Apagada por cascade quando o `match_summary`
e wiped na recepcao de evento.

### `occurrence_snapshot`

Linha por evento registado (`GOAL`, `OWN_GOAL`, `YELLOW_CARD`,
`RED_CARD`, `SUBSTITUTION`, `FOUL`). PK `occurrence_id` (vem do
match-service).

### `ticket_sale`

Linha por bilhete pago. Atualizada com `validated_at` quando o
`ticket.ticket.validated` chega.

## Endpoints

### Historico de fichas tecnicas

| Method | Path | Role |
|--------|------|------|
| GET | `/api/v1/stats/match-sheets` | publico |
| GET | `/api/v1/stats/match-sheets/{matchId}` | publico |

**Filtros** (`/match-sheets`):

| Query | Tipo | Descricao |
|-------|------|-----------|
| `clubId` | int64 | matches onde o clube foi home OU away |
| `playerId` | int64 | matches em que o jogador apareceu no lineup |
| `season` | string (`YYYY-YY`) | epoca futebolistica (Jul-Jun) |
| `status` | enum | status do match no momento do close |
| `page`, `size`, `sort` | pagination | envelope padrao |

Implementacao: `MatchSummarySpecs` compõe predicados JPA Criteria
dinamicamente. O filtro `playerId` usa subquery `EXISTS` sobre
`lineup_snapshot`.

### Relatorios de vendas

| Method | Path | Role | Descricao |
|--------|------|------|-----------|
| GET | `/api/v1/stats/sales/summary` | platform-admin | total agregado (sold, validated, revenue, rate) |
| GET | `/api/v1/stats/sales/by-match/{matchId}` | platform-admin | agregado para um match |
| GET | `/api/v1/stats/sales/range?from=...&to=...` | platform-admin | janela temporal |

`from` e inclusivo, `to` e exclusivo. `validationRate` no endpoint
`range` e sempre 0 (validacao acontece depois do paid e enviesa a
metrica em janelas curtas).

## Eventos consumidos

Contratos em [events/async-contracts.md](events/async-contracts.md).

| Routing key | Queue | Acao |
|-------------|-------|------|
| `match.sheet.closed` | `stats.match-sheet-closed` | UPSERT MatchSummary + lineup + occurrences |
| `ticket.ticket.paid` | `stats.ticket-paid` | UPSERT TicketSale |
| `ticket.ticket.validated` | `stats.ticket-validated` | UPDATE TicketSale.validatedAt |

### Idempotencia

`onMatchSheetClosed` apaga linhas anteriores do match antes de inserir
as novas:

```kotlin
lineupRepo.deleteByIdMatchId(event.matchId)
occurrenceRepo.deleteByMatchId(event.matchId)
summaryRepo.deleteById(event.matchId)
summaryRepo.flush()
// then insert
```

`onTicketPaid` faz delete-then-insert. `onTicketValidated` so atualiza
a coluna `validated_at` da TicketSale existente — se a TicketSale
ainda nao existe (out-of-order delivery), o evento e ignorado e
logado como warning.

## Migracoes Flyway

| Versao | Conteudo |
|--------|----------|
| V1 | `match_summary` + `lineup_snapshot` + `occurrence_snapshot` |
| V2 | `ticket_sale` |

## Trabalho futuro

- [x] ~~Implementar o producer no **ticket-service** (`TicketPaid` e
      `TicketValidated`)~~ — concluido na P3
- [ ] Dead-letter queue + retry exponencial para mensagens que falham
- [ ] Tracing distribuido (traceId/spanId nos eventos)
- [ ] Reports avancados (top scorers, cleansheets, atendance trends)
