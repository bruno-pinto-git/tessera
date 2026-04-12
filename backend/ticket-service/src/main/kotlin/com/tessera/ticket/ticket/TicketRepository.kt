package com.tessera.ticket.ticket

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TicketRepository : JpaRepository<Ticket, Long> {
    fun findByCode(code: UUID): Ticket?
}
