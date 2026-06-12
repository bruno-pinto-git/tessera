-- Home club of the match the ticket was sold for, so sales can be aggregated
-- per club (e.g. a manager's club dashboard). Populated from the home_club_id
-- now carried on the ticket.ticket.paid event.
ALTER TABLE ticket_sale ADD COLUMN home_club_id BIGINT;
CREATE INDEX idx_ticket_sale_home_club ON ticket_sale (home_club_id);
