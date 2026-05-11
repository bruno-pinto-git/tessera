-- =============================================================================
-- V1__match_history.sql
-- Read-side tables fed by the `match.sheet.closed` event published by the
-- match-service. We denormalize aggressively so queries are simple JOINs or
-- single-table scans.
-- =============================================================================

-- One row per match that has had its sheet closed. Idempotent: re-receiving
-- an event for the same match REPLACES the snapshot (DELETE + INSERT in the
-- consumer).
CREATE TABLE match_summary (
    match_id        BIGINT         PRIMARY KEY,
    season          VARCHAR(10)    NOT NULL,
    match_status    VARCHAR(20)    NOT NULL,
    kickoff_at      TIMESTAMPTZ    NOT NULL,
    home_team_id    BIGINT         NOT NULL,
    away_team_id    BIGINT         NOT NULL,
    home_club_id    BIGINT         NOT NULL,
    away_club_id    BIGINT         NOT NULL,
    venue_id        BIGINT,
    home_score      INTEGER,
    away_score      INTEGER,
    referee_name    VARCHAR(200),
    snapshot_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_match_summary_season       ON match_summary(season);
CREATE INDEX idx_match_summary_home_club    ON match_summary(home_club_id);
CREATE INDEX idx_match_summary_away_club    ON match_summary(away_club_id);
CREATE INDEX idx_match_summary_status       ON match_summary(match_status);
CREATE INDEX idx_match_summary_kickoff_at   ON match_summary(kickoff_at);


-- Snapshot of the lineup at sheet-close time.
CREATE TABLE lineup_snapshot (
    match_id        BIGINT         NOT NULL REFERENCES match_summary(match_id) ON DELETE CASCADE,
    player_id       BIGINT         NOT NULL,
    team_id         BIGINT         NOT NULL,
    shirt_number    INTEGER,
    role            VARCHAR(10)    NOT NULL,
    PRIMARY KEY (match_id, player_id),
    CONSTRAINT chk_lineup_snapshot_role
        CHECK (role IN ('STARTER','SUBSTITUTE'))
);

CREATE INDEX idx_lineup_snapshot_player ON lineup_snapshot(player_id);
CREATE INDEX idx_lineup_snapshot_team   ON lineup_snapshot(team_id);


-- Snapshot of every in-game occurrence at sheet-close time.
CREATE TABLE occurrence_snapshot (
    occurrence_id       BIGINT         PRIMARY KEY,
    match_id            BIGINT         NOT NULL REFERENCES match_summary(match_id) ON DELETE CASCADE,
    minute              INTEGER        NOT NULL,
    type                VARCHAR(20)    NOT NULL,
    team_id             BIGINT         NOT NULL,
    player_id           BIGINT         NOT NULL,
    replaced_player_id  BIGINT,
    CONSTRAINT chk_occurrence_snapshot_type
        CHECK (type IN ('GOAL','OWN_GOAL','YELLOW_CARD','RED_CARD','SUBSTITUTION','FOUL'))
);

CREATE INDEX idx_occurrence_snapshot_match  ON occurrence_snapshot(match_id);
CREATE INDEX idx_occurrence_snapshot_player ON occurrence_snapshot(player_id);
CREATE INDEX idx_occurrence_snapshot_team   ON occurrence_snapshot(team_id);
CREATE INDEX idx_occurrence_snapshot_type   ON occurrence_snapshot(type);
