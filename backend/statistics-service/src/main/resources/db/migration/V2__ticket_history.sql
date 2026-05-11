-- =============================================================================
-- V2__ticket_history.sql
-- Read-side tables for ticket sales, fed by the ticket-service via:
--   - `ticket.ticket.paid`        → upsert ticket_sale
--   - `ticket.ticket.validated`   → update ticket_sale.validated_at
-- =============================================================================

CREATE TABLE ticket_sale (
    ticket_id        BIGINT          PRIMARY KEY,
    event_id         BIGINT          NOT NULL,
    match_id         BIGINT          NOT NULL,
    price            NUMERIC(8, 2)   NOT NULL,
    payment_method   VARCHAR(20),
    paid_at          TIMESTAMPTZ     NOT NULL,
    validated_at     TIMESTAMPTZ
);

CREATE INDEX idx_ticket_sale_match   ON ticket_sale(match_id);
CREATE INDEX idx_ticket_sale_event   ON ticket_sale(event_id);
CREATE INDEX idx_ticket_sale_paid_at ON ticket_sale(paid_at);
