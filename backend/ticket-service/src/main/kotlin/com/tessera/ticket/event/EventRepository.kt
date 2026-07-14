package com.tessera.ticket.event

import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository : JpaRepository<Event, Long> {
    /** True if a non-cancelled box office already exists for the given match. */
    fun existsByMatchIdAndStatusNot(matchId: Long, status: String): Boolean
}
