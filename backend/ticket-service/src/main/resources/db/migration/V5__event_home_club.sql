-- Home club of the match, snapshotted when the box office is opened, so the
-- ticket.ticket.paid event can carry it for per-club sales aggregation without
-- resolving it via a match-service HTTP call at payment time.
ALTER TABLE event ADD COLUMN home_club_id BIGINT;
