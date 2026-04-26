package com.tessera.ticket.event

import org.springframework.data.jpa.repository.JpaRepository

interface EventRepository : JpaRepository<Event, Long>
