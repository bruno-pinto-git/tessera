-- =============================================================================
-- V6__match.sql
-- match-service: match table.
--
-- "match" is a reserved word in some contexts so we quote it where needed,
-- but in PostgreSQL it is fine to use as an unquoted identifier.
-- =============================================================================

CREATE TABLE match (
    id            BIGSERIAL      PRIMARY KEY,
    home_team_id  BIGINT         NOT NULL REFERENCES team(id),
    away_team_id  BIGINT         NOT NULL REFERENCES team(id),
    venue_id      BIGINT         REFERENCES venue(id),
    kickoff_at    TIMESTAMPTZ    NOT NULL,
    status        VARCHAR(20)    NOT NULL DEFAULT 'SCHEDULED',
    home_score    INTEGER,
    away_score    INTEGER,
    referee_name  VARCHAR(200),
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at    TIMESTAMPTZ,
    CONSTRAINT chk_match_teams_distinct
        CHECK (home_team_id <> away_team_id),
    CONSTRAINT chk_match_status
        CHECK (status IN ('SCHEDULED','LIVE','FINISHED','POSTPONED','ABANDONED')),
    CONSTRAINT chk_match_home_score
        CHECK (home_score IS NULL OR home_score >= 0),
    CONSTRAINT chk_match_away_score
        CHECK (away_score IS NULL OR away_score >= 0),
    CONSTRAINT chk_match_finished_has_score
        CHECK (status <> 'FINISHED' OR (home_score IS NOT NULL AND away_score IS NOT NULL))
);

CREATE INDEX idx_match_kickoff_at  ON match(kickoff_at);
CREATE INDEX idx_match_status      ON match(status);
CREATE INDEX idx_match_home_team   ON match(home_team_id);
CREATE INDEX idx_match_away_team   ON match(away_team_id);
CREATE INDEX idx_match_venue       ON match(venue_id);
CREATE INDEX idx_match_deleted_at  ON match(deleted_at);
