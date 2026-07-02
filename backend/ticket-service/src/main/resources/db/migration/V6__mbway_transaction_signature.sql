-- =============================================================================
-- V6__mbway_transaction_signature.sql
--
-- SIBS requires the `transactionSignature` returned by checkout creation on
-- every subsequent call for that transaction (the purchase trigger AND
-- status polling). We already store mbway_transaction_id for correlation;
-- this adds the signature alongside it so a later status poll (triggered by
-- reading the ticket — see TicketService.getByIdRefreshed) can authenticate
-- without redoing checkout.
-- =============================================================================

ALTER TABLE ticket
    ADD COLUMN mbway_transaction_signature VARCHAR(256);
