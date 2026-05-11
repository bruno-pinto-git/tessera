-- =============================================================================
-- V2__venue.sql
-- match-service: venue table (stadiums and grounds where matches are played).
--
-- Soft-delete via deleted_at; queries filter explicitly.
-- =============================================================================

CREATE TABLE venue (
    id          BIGSERIAL      PRIMARY KEY,
    name        VARCHAR(200)   NOT NULL,
    capacity    INTEGER        NOT NULL,
    address     VARCHAR(500),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT chk_venue_name_length
        CHECK (char_length(name) BETWEEN 2 AND 200),
    CONSTRAINT chk_venue_capacity
        CHECK (capacity BETWEEN 0 AND 200000)
);

-- Unique active name (allows reusing a name after a venue is soft-deleted).
CREATE UNIQUE INDEX idx_venue_name_active
    ON venue (LOWER(name))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_venue_deleted_at ON venue(deleted_at);
