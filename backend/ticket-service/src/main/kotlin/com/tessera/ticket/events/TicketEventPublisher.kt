package com.tessera.ticket.events

import com.tessera.ticket.ticket.Ticket
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Publishes ticket domain events.
 *
 * Called from inside service-layer transactions via `publishOnCommit`
 * so the broker only sees the event after the DB write commits — this
 * guarantees statistics-service never builds a sale row for a ticket
 * whose payment rolled back.
 *
 * Routing keys follow `docs/events/async-contracts.md`.
 */
@Component
class TicketEventPublisher(
    private val rabbit: RabbitTemplate,
    @Value("\${tessera.events.exchange}") private val exchange: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publishTicketPaid(ticket: Ticket) {
        val ev = ticket.event ?: run {
            log.warn("Cannot publish ticket.paid: ticket {} has no event", ticket.id)
            return
        }
        val payload = TicketPaidEvent(
            occurredAt    = OffsetDateTime.now(ZoneOffset.UTC),
            ticketId      = ticket.id,
            eventId       = ev.id,
            matchId       = ev.matchId,
            price         = ticket.price,
            paymentMethod = ticket.paymentMethod,
            paidAt        = ticket.paymentDate ?: OffsetDateTime.now(ZoneOffset.UTC),
        )
        try {
            rabbit.convertAndSend(exchange, ROUTING_PAID, payload)
            log.info("Published ticket.ticket.paid ticketId={}", ticket.id)
        } catch (e: Exception) {
            log.error("Failed to publish ticket.ticket.paid for ticketId={}", ticket.id, e)
        }
    }

    fun publishTicketValidated(ticket: Ticket) {
        val payload = TicketValidatedEvent(
            occurredAt   = OffsetDateTime.now(ZoneOffset.UTC),
            ticketId     = ticket.id,
            matchId      = ticket.event?.matchId,
            validatedAt  = ticket.validationDate ?: OffsetDateTime.now(ZoneOffset.UTC),
            validatorSub = ticket.validatorSub,
        )
        try {
            rabbit.convertAndSend(exchange, ROUTING_VALIDATED, payload)
            log.info("Published ticket.ticket.validated ticketId={}", ticket.id)
        } catch (e: Exception) {
            log.error("Failed to publish ticket.ticket.validated for ticketId={}", ticket.id, e)
        }
    }

    companion object {
        const val ROUTING_PAID = "ticket.ticket.paid"
        const val ROUTING_VALIDATED = "ticket.ticket.validated"
    }
}
