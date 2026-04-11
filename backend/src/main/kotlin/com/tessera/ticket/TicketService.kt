package com.tessera.ticket

import com.tessera.event.EventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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
}
