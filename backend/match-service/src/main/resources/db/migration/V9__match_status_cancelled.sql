-- =============================================================================
-- V9__match_status_cancelled.sql
-- Add CANCELLED as a valid match status.
--
-- Semantics:
--   - SCHEDULED  -> CANCELLED   (allowed; admin pulls the plug before kickoff)
--   - CANCELLED is terminal; auto-locks the match-sheet (same as FINISHED).
-- =============================================================================

ALTER TABLE match DROP CONSTRAINT chk_match_status;

ALTER TABLE match
    ADD CONSTRAINT chk_match_status
    CHECK (status IN ('SCHEDULED','LIVE','FINISHED','POSTPONED','ABANDONED','CANCELLED'));
