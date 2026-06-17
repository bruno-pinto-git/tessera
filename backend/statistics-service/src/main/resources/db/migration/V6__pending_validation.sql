-- Holds a ticket.ticket.validated that arrived before its ticket.ticket.paid
-- (out-of-order delivery across separate queues). When the paid event lands,
-- the consumer drains this row onto the sale so the validation isn't lost.
CREATE TABLE pending_validation (
    ticket_id     BIGINT      PRIMARY KEY,
    validated_at  TIMESTAMPTZ NOT NULL
);
