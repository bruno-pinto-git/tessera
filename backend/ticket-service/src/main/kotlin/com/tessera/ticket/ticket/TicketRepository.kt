package com.tessera.ticket.ticket

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByCode(code: UUID): Ticket?
    fun findByOwnerSub(ownerSub: String, pageable: Pageable): Page<Ticket>
    fun findByEventId(eventId: Long, pageable: Pageable): Page<Ticket>
}
