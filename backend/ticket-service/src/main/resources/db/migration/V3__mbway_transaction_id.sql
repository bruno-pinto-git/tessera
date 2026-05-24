-- =============================================================================
-- V3__mbway_transaction_id.sql
--
-- Adds the gateway-side transaction id returned by the MB WAY gateway
-- (SIBS Payment Gateway in production; our local mock-mbway app in dev) when
-- a payment is initiated.
--
-- We need this to correlate the asynchronous merchant-callback webhook back
-- to the ticket that originated the payment, without depending on the
-- ambiguous mbway_reference column (which carries the customer's phone
-- number, not the transaction id).
-- =============================================================================

ALTER TABLE ticket
    ADD COLUMN mbway_transaction_id VARCHAR(64);

CREATE INDEX idx_ticket_mbway_transaction_id
    ON ticket(mbway_transaction_id)
    WHERE mbway_transaction_id IS NOT NULL;
