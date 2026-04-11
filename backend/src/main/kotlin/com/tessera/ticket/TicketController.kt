package com.tessera.ticket

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

data class CreateTicketRequest(
    val eventId: Long,
    val supporter: Boolean = false
)

data class TicketResponse(
    val id: Long,
    val code: String,
    val price: String,
    val status: String,
    val createdAt: String
)

@RestController
@RequestMapping("/api/tickets")
class TicketController(
    private val ticketService: TicketService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody request: CreateTicketRequest): TicketResponse {
        val ticket = ticketService.create(request.eventId, request.supporter)
        return TicketResponse(
            id = ticket.id,
            code = ticket.code.toString(),
            price = ticket.price.toString(),
            status = ticket.status,
            createdAt = ticket.createdAt.toString()
        )
    }
}
