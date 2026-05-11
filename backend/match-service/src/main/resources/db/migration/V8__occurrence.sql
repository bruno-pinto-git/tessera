-- =============================================================================
-- V8__occurrence.sql
-- match-service: occurrence table — events recorded in the match sheet
-- (goals, cards, substitutions). Hard delete (rebuilt freely while
-- the sheet is unlocked).
-- =============================================================================

CREATE TABLE occurrence (
    id                  BIGSERIAL      PRIMARY KEY,
    match_sheet_id      BIGINT         NOT NULL REFERENCES match_sheet(id) ON DELETE CASCADE,
    minute              INTEGER        NOT NULL,
    type                VARCHAR(20)    NOT NULL,
    team_id             BIGINT         NOT NULL REFERENCES team(id),
    player_id           BIGINT         NOT NULL REFERENCES player(id),
    replaced_player_id  BIGINT         REFERENCES player(id),
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_occurrence_minute
        CHECK (minute BETWEEN 0 AND 200),
    CONSTRAINT chk_occurrence_type
        CHECK (type IN ('GOAL','OWN_GOAL','YELLOW_CARD','RED_CARD','SUBSTITUTION')),
    -- SUBSTITUTION requires replaced_player_id; others must not have it
    CONSTRAINT chk_occurrence_substitution_coherent
        CHECK (
            (type = 'SUBSTITUTION' AND replaced_player_id IS NOT NULL) OR
            (type <> 'SUBSTITUTION' AND replaced_player_id IS NULL)
        ),
    -- The on-field and off-field players cannot be the same person
    CONSTRAINT chk_occurrence_different_players
        CHECK (replaced_player_id IS NULL OR replaced_player_id <> player_id)
);

CREATE INDEX idx_occurrence_sheet  ON occurrence(match_sheet_id);
CREATE INDEX idx_occurrence_team   ON occurrence(team_id);
CREATE INDEX idx_occurrence_player ON occurrence(player_id);
