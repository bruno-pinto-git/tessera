-- =============================================================================
-- V7__stripe_checkout.sql
--
-- Adds the Stripe Checkout Session id (correlates a CARD ticket to its
-- Stripe-hosted checkout page, for status polling — there is no reachable
-- webhook endpoint) and drops CASH as an accepted payment method: it never
-- had a real gateway behind it (same instant-PAID code path as CARD), and
-- the on-site "pay in cash" option is being removed from the product.
-- =============================================================================

ALTER TABLE ticket
    ADD COLUMN stripe_checkout_session_id VARCHAR(255);

CREATE INDEX idx_ticket_stripe_checkout_session_id
    ON ticket(stripe_checkout_session_id)
    WHERE stripe_checkout_session_id IS NOT NULL;

-- A CHECK constraint validates every row, not just new writes — reclassify
-- any historical CASH tickets before re-adding the constraint without it,
-- or the migration would fail outright on an environment with old demo data.
UPDATE ticket SET payment_method = 'CARD' WHERE payment_method = 'CASH';

ALTER TABLE ticket DROP CONSTRAINT chk_ticket_payment_method;
ALTER TABLE ticket
    ADD CONSTRAINT chk_ticket_payment_method
        CHECK (payment_method IS NULL OR payment_method IN ('MBWAY', 'CARD'));
