package com.tessera.ticket.ticket

import com.tessera.ticket.event.EventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val eventRepository: EventRepository
) {

    @Transactional
    fun create(eventId: Long, supporter: Boolean): Ticket {
        val event = eventRepository.findById(eventId)
            .orElseThrow { IllegalArgumentException("Event not found: $eventId") }

        val price = if (supporter) event.priceSupporter else event.priceNormal

        val ticket = Ticket(
            event = event,
            price = price
        )

        return ticketRepository.save(ticket)
    }

    @Transactional
    fun validate(code: UUID): Ticket {
        val ticket = ticketRepository.findByCode(code)
            ?: throw TicketNotFoundException("Ticket not found: $code")

        if (ticket.status != TicketStatus.PAID) {
            throw InvalidTicketStatusException("Ticket status is ${ticket.status}, expected ${TicketStatus.PAID}")
        }

        ticket.status = TicketStatus.VALIDATED
        ticket.validationDate = OffsetDateTime.now()

        return ticketRepository.save(ticket)
    }
}

class TicketNotFoundException(message: String) : RuntimeException(message)
class InvalidTicketStatusException(message: String) : RuntimeException(message)
