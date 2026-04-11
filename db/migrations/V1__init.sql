-- =============================================================================
-- V1__init.sql
-- Phase 1: event, ticket
--
-- Domain flow:
--   event 1:N ticket
--   ticket lifecycle: PENDING -> PAID -> VALIDATED (or deleted)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- =============================================================================
-- EVENT
-- =============================================================================
CREATE TABLE event (
    id               BIGSERIAL      PRIMARY KEY,
    match_id         BIGINT,
    name             VARCHAR(255),
    price_normal     NUMERIC(8, 2)  NOT NULL DEFAULT 0,
    price_supporter  NUMERIC(8, 2)  NOT NULL DEFAULT 0,
    status           VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT chk_event_price_normal
        CHECK (price_normal >= 0),
    CONSTRAINT chk_event_price_supporter
        CHECK (price_supporter >= 0),
    CONSTRAINT chk_event_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'SALES_CLOSED', 'CANCELLED'))
);

CREATE INDEX idx_event_status ON event(status);


-- =============================================================================
-- TICKET
-- `code` is the UUID encoded into the QR code used at the gate.
-- Lifecycle: PENDING -> PAID -> VALIDATED (or row is deleted)
-- =============================================================================
CREATE TABLE ticket (
    id               BIGSERIAL      PRIMARY KEY,
    event_id         BIGINT         NOT NULL REFERENCES event(id),
    code             UUID           NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    price            NUMERIC(8, 2)  NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    payment_method   VARCHAR(20),
    mbway_reference  VARCHAR(100),
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    payment_date     TIMESTAMPTZ,
    validation_date  TIMESTAMPTZ,
    validator_id     BIGINT,
    CONSTRAINT chk_ticket_price
        CHECK (price >= 0),
    CONSTRAINT chk_ticket_status
        CHECK (status IN ('PENDING', 'PAID', 'VALIDATED')),
    CONSTRAINT chk_ticket_payment_method
        CHECK (payment_method IS NULL OR payment_method IN ('MBWAY', 'CARD', 'CASH')),
    CONSTRAINT chk_ticket_payment_coherent
        CHECK (
            (status IN ('PAID', 'VALIDATED') AND payment_date IS NOT NULL)
            OR (status = 'PENDING' AND payment_date IS NULL)
        ),
    CONSTRAINT chk_ticket_validation_coherent
        CHECK (
            (status = 'VALIDATED' AND validation_date IS NOT NULL)
            OR (status <> 'VALIDATED' AND validation_date IS NULL)
        )
);

CREATE INDEX idx_ticket_event  ON ticket(event_id);
CREATE INDEX idx_ticket_status ON ticket(status);
