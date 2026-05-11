-- =============================================================================
-- V1__init.sql
-- match-service schema: clubs (and later teams, players, venues, matches, ...)
--
-- Soft-delete strategy: deleted_at TIMESTAMPTZ; queries filter explicitly.
-- =============================================================================


-- =============================================================================
-- CLUB
-- =============================================================================
CREATE TABLE club (
    id            BIGSERIAL      PRIMARY KEY,
    name          VARCHAR(200)   NOT NULL,
    founded_year  INTEGER,
    crest_url     VARCHAR(500),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT chk_club_name_length
        CHECK (char_length(name) BETWEEN 2 AND 200),
    CONSTRAINT chk_club_founded_year
        CHECK (founded_year IS NULL OR founded_year BETWEEN 1850 AND 2100)
);

-- Unique active name (allows reusing a name after a club is soft-deleted).
CREATE UNIQUE INDEX idx_club_name_active
    ON club (LOWER(name))
    WHERE deleted_at IS NULL;

CREATE INDEX idx_club_deleted_at ON club(deleted_at);
