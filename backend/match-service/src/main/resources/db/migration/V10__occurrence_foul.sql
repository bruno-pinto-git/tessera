-- =============================================================================
-- V10__occurrence_foul.sql
-- Adds FOUL to the set of valid occurrence types.
--
-- Semantics: a foul is a single tactical event recorded on the sheet (e.g.
-- "no minuto 67, jogador X cometeu falta na linha de defesa"). It does NOT
-- imply a card — cards remain separate occurrences (YELLOW_CARD / RED_CARD).
-- replaced_player_id stays NULL for fouls (same as goals/cards).
-- =============================================================================

ALTER TABLE occurrence DROP CONSTRAINT chk_occurrence_type;

ALTER TABLE occurrence
    ADD CONSTRAINT chk_occurrence_type
    CHECK (type IN (
        'GOAL','OWN_GOAL','YELLOW_CARD','RED_CARD','SUBSTITUTION','FOUL'
    ));
