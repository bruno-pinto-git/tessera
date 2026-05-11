-- =============================================================================
-- V3__ticket_sale_nullable_match.sql
--
-- Events in the ticket-service can be sold without being attached to a
-- specific match (`event.match_id` is nullable). Relax the read-side mirror
-- to match.
-- =============================================================================

ALTER TABLE ticket_sale ALTER COLUMN match_id DROP NOT NULL;
