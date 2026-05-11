-- =============================================================================
-- V7__match_sheet_lineup.sql
-- match-service: match_sheet (1:1 with match) + lineup_entry.
--
-- Lock semantics: edits allowed iff locked = false AND match.status IN
-- ('SCHEDULED','LIVE'). Enforced in the service layer.
-- =============================================================================


CREATE TABLE match_sheet (
    id          BIGSERIAL      PRIMARY KEY,
    match_id    BIGINT         NOT NULL UNIQUE REFERENCES match(id),
    locked      BOOLEAN        NOT NULL DEFAULT FALSE,
    locked_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_match_sheet_locked_coherent
        CHECK ((locked = TRUE AND locked_at IS NOT NULL) OR
               (locked = FALSE AND locked_at IS NULL))
);


CREATE TABLE lineup_entry (
    match_sheet_id  BIGINT       NOT NULL REFERENCES match_sheet(id) ON DELETE CASCADE,
    player_id       BIGINT       NOT NULL REFERENCES player(id),
    team_id         BIGINT       NOT NULL REFERENCES team(id),
    shirt_number    INTEGER,
    role            VARCHAR(10)  NOT NULL,
    PRIMARY KEY (match_sheet_id, player_id),
    CONSTRAINT chk_lineup_role
        CHECK (role IN ('STARTER','SUBSTITUTE')),
    CONSTRAINT chk_lineup_shirt_number
        CHECK (shirt_number IS NULL OR shirt_number BETWEEN 1 AND 99)
);

-- Within a single match-sheet, each shirt number is unique per team
-- (when a number is set).
CREATE UNIQUE INDEX idx_lineup_sheet_team_shirt
    ON lineup_entry (match_sheet_id, team_id, shirt_number)
    WHERE shirt_number IS NOT NULL;

CREATE INDEX idx_lineup_sheet ON lineup_entry(match_sheet_id);
CREATE INDEX idx_lineup_team  ON lineup_entry(team_id);
