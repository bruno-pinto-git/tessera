package com.tessera.ticket.ticket

import com.tessera.ticket.event.EventRepository
import com.tessera.ticket.events.TicketEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository,
    private val publisher: TicketEventPublisher,
) {

    @Transactional
    fun create(eventId: Long, supporter: Boolean, ownerSub: String): Ticket {
        val event = eventRepository.findById(eventId)
            .orElseThrow { EventNotFoundException("Event not found: $eventId") }

        val price = if (supporter) event.priceSupporter else event.priceNormal

        val ticket = Ticket(
            event = event,
            price = price,
            ownerSub = ownerSub,
        )

        return ticketRepository.save(ticket)
    }

    @Transactional(readOnly = true)
    fun getById(id: Long): Ticket =
        ticketRepository.findById(id)
            .orElseThrow { TicketNotFoundException("Ticket not found: $id") }

    @Transactional(readOnly = true)
    fun findByOwner(ownerSub: String, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByOwnerSub(ownerSub, pageable)

    @Transactional(readOnly = true)
    fun findByEvent(eventId: Long, pageable: Pageable): Page<Ticket> =
        ticketRepository.findByEventId(eventId, pageable)

    /**
     * Transition PENDING → PAID. Stamps the chosen payment method, an optional
     * MB WAY reference, and the `paymentDate`. Publishes `ticket.ticket.paid`
     * once the surrounding transaction commits.
     */
    @Transactional
    fun pay(id: Long, paymentMethod: String, mbwayReference: String?): Ticket {
        val ticket = ticketRepository.findById(id)
            .orElseThrow { TicketNotFoundException("Ticket not found: $id") }

        if (ticket.status != TicketStatus.PENDING) {
            throw InvalidTicketStatusException(
                "Cannot pay ticket in status ${ticket.status} (expected PENDING)"
            )
        }
        val method = paymentMethod.uppercase()
        if (method !in ALLOWED_PAYMENT_METHODS) {
            throw IllegalArgumentException(
                "Invalid payment method '$paymentMethod'; expected one of $ALLOWED_PAYMENT_METHODS"
            )
        }

        ticket.status = TicketStatus.PAID
        ticket.paymentMethod = method
        ticket.mbwayReference = mbwayReference
        ticket.paymentDate = OffsetDateTime.now()

        val saved = ticketRepository.save(ticket)
        publishOnCommit { publisher.publishTicketPaid(saved) }
        return saved
    }

    /**
     * Transition PAID → VALIDATED. The validator (staff/admin) is identified
     * by the Keycloak subject UUID from the JWT. Publishes
     * `ticket.ticket.validated` once the surrounding transaction commits.
     */
    @Transactional
    fun validate(code: UUID, validatorSub: String): Ticket {
        val ticket = ticketRepository.findByCode(code)
            ?: throw TicketNotFoundException("Ticket not found: $code")

        if (ticket.status != TicketStatus.PAID) {
            throw InvalidTicketStatusException(
                "Ticket status is ${ticket.status}, expected ${TicketStatus.PAID}"
            )
        }

        ticket.status = TicketStatus.VALIDATED
        ticket.validationDate = OffsetDateTime.now()
        ticket.validatorSub = validatorSub

        val saved = ticketRepository.save(ticket)
        publishOnCommit { publisher.publishTicketValidated(saved) }
        return saved
    }

    private fun publishOnCommit(action: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                object : TransactionSynchronization {
                    override fun afterCommit() { action() }
                }
            )
        } else {
            action()
        }
    }

    companion object {
        val ALLOWED_PAYMENT_METHODS = setOf("MBWAY", "CARD", "CASH")
    }
}

class TicketNotFoundException(message: String) : RuntimeException(message)
class EventNotFoundException(message: String) : RuntimeException(message)
class InvalidTicketStatusException(message: String) : RuntimeException(message)
