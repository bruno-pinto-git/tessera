-- =============================================================================
-- V4__player.sql
-- match-service: player table.
--
-- Constraints:
--   * shirt_number unique per team among ACTIVE players (when set)
--   * height/weight in plausible ranges
-- =============================================================================

CREATE TABLE player (
    id              BIGSERIAL      PRIMARY KEY,
    team_id         BIGINT         NOT NULL REFERENCES team(id),
    first_name      VARCHAR(100)   NOT NULL,
    last_name       VARCHAR(100)   NOT NULL,
    birthdate       DATE,
    nationality     CHAR(3),
    position        VARCHAR(2)     NOT NULL,
    shirt_number    INTEGER,
    photo_url       VARCHAR(500),
    dominant_foot   VARCHAR(5),
    height          INTEGER,
    weight          INTEGER,
    status          VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT chk_player_first_name_length
        CHECK (char_length(first_name) BETWEEN 1 AND 100),
    CONSTRAINT chk_player_last_name_length
        CHECK (char_length(last_name) BETWEEN 1 AND 100),
    CONSTRAINT chk_player_nationality
        CHECK (nationality IS NULL OR nationality ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_player_position
        CHECK (position IN ('GK','DF','MF','FW')),
    CONSTRAINT chk_player_shirt_number
        CHECK (shirt_number IS NULL OR shirt_number BETWEEN 1 AND 99),
    CONSTRAINT chk_player_dominant_foot
        CHECK (dominant_foot IS NULL OR dominant_foot IN ('LEFT','RIGHT','BOTH')),
    CONSTRAINT chk_player_height
        CHECK (height IS NULL OR height BETWEEN 100 AND 250),
    CONSTRAINT chk_player_weight
        CHECK (weight IS NULL OR weight BETWEEN 30 AND 200),
    CONSTRAINT chk_player_status
        CHECK (status IN ('ACTIVE','INJURED','SUSPENDED'))
);

-- shirt number unique per team among ACTIVE (non-deleted, with shirt set) players
CREATE UNIQUE INDEX idx_player_team_shirt_active
    ON player (team_id, shirt_number)
    WHERE deleted_at IS NULL AND shirt_number IS NOT NULL;

CREATE INDEX idx_player_team        ON player(team_id);
CREATE INDEX idx_player_deleted_at  ON player(deleted_at);
