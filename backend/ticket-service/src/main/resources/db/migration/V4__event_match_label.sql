-- Snapshot of the fixture ("Home vs Away") taken when the box office opens, so
-- a ticket keeps showing the teams even after the match is deleted upstream.
ALTER TABLE event ADD COLUMN match_label VARCHAR(255);
