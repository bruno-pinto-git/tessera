-- =============================================================================
-- V5__player_nationality_varchar.sql
-- Convert player.nationality from CHAR(3) to VARCHAR(3) so the schema matches
-- the JPA entity (which uses @Column(length = 3) → VARCHAR).
--
-- bpchar (CHAR) right-pads with spaces, which differs from VARCHAR semantics.
-- =============================================================================

ALTER TABLE player
    ALTER COLUMN nationality TYPE VARCHAR(3);
