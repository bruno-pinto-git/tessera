-- Backfill home_club_id for sales recorded before V4 (and any that arrived with
-- a null club). We can only resolve it for matches that already have a closed
-- match-sheet snapshot (match_summary); sales for matches without one stay null
-- until a new paid event re-populates them. Best-effort reconciliation.
UPDATE ticket_sale ts
SET home_club_id = ms.home_club_id
FROM match_summary ms
WHERE ts.match_id = ms.match_id
  AND ts.home_club_id IS NULL;
