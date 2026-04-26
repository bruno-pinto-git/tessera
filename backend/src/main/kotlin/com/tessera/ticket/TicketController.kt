package com.tessera.ticket

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

private val log = LoggerFactory.getLogger(TicketController::class.java)

data class CreateTicketRequest(
    val eventId: Long,
    val supporter: Boolean = false
)

data class ValidateTicketRequest(
    val code: String
)

data class TicketResponse(
    val id: Long,
    val code: String,
    val price: String,
    val status: String,
    val createdAt: String
)

data class ErrorResponse(
    val error: String
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
        return toResponse(ticket)
    }

    @PostMapping("/validate")
    fun validate(@RequestBody request: ValidateTicketRequest): ResponseEntity<Any> {
        log.info("Validate request received - code: ${request.code}")
        return try {
            val uuid = UUID.fromString(request.code)
            val ticket = ticketService.validate(uuid)
            log.info("Ticket validated successfully - id: ${ticket.id}, status: ${ticket.status}")
            ResponseEntity.ok(toResponse(ticket))
        } catch (e: TicketNotFoundException) {
            log.warn("Ticket not found - code: ${request.code}")
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "Ticket not found"))
        } catch (e: InvalidTicketStatusException) {
            log.warn("Invalid ticket status - code: ${request.code}, error: ${e.message}")
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse(e.message ?: "Invalid ticket status"))
        } catch (e: IllegalArgumentException) {
            log.warn("Invalid UUID format - code: ${request.code}")
            ResponseEntity.badRequest()
                .body(ErrorResponse("Invalid UUID format"))
        }
    }

    private fun toResponse(ticket: Ticket) = TicketResponse(
        id = ticket.id,
        code = ticket.code.toString(),
        price = ticket.price.toString(),
        status = ticket.status.name,
        createdAt = ticket.createdAt.toString()
    )
}
