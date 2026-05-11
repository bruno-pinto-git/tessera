-- =============================================================================
-- V3__team.sql
-- match-service: team table (one team per category per club).
-- =============================================================================

CREATE TABLE team (
    id          BIGSERIAL      PRIMARY KEY,
    club_id     BIGINT         NOT NULL REFERENCES club(id),
    category    VARCHAR(20)    NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT chk_team_category
        CHECK (category IN (
            'SENIOR_M','SENIOR_F','SUB_23','SUB_19','SUB_17','SUB_15',
            'SUB_13','SUB_11','SUB_9','SUB_7','VETERANS','OTHER'
        ))
);

-- A club can have only one ACTIVE team per category.
CREATE UNIQUE INDEX idx_team_club_category_active
    ON team (club_id, category)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_team_club ON team(club_id);
CREATE INDEX idx_team_deleted_at ON team(deleted_at);
