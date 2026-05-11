-- =============================================================================
-- V2__ticket_owner_validator.sql
--
-- Adds JWT-subject ownership tracking to tickets:
--   - owner_sub      : the Keycloak subject UUID of the buyer (mandatory for
--                      newly-created tickets; legacy rows have NULL)
--   - validator_sub  : the Keycloak subject UUID of the staff member who
--                      scanned the ticket at the gate
--
-- We also drop the legacy `validator_id BIGINT` column, which never had any
-- producer (there is no users table inside ticket-service to reference).
-- =============================================================================

ALTER TABLE ticket
    ADD COLUMN owner_sub     VARCHAR(64),
    ADD COLUMN validator_sub VARCHAR(64);

ALTER TABLE ticket DROP COLUMN validator_id;

CREATE INDEX idx_ticket_owner_sub ON ticket(owner_sub);

-- Validation coherence: when status=VALIDATED, validator_sub must be set.
-- We do NOT enforce this on legacy rows (status=VALIDATED with no validator)
-- because they were created before this column existed; new validations always
-- set both.
ALTER TABLE ticket
    ADD CONSTRAINT chk_ticket_validator_coherent
        CHECK (
            status <> 'VALIDATED'
            OR validator_sub IS NOT NULL
            OR validation_date < TIMESTAMPTZ '2026-05-11 00:00:00+00'
        );
