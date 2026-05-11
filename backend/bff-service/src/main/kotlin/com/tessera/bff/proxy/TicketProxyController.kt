package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Proxy for ticket-service. Old paths under /api/tickets are kept for the
 * existing frontend wiring; we will migrate to /api/v1/tickets once the
 * ticket-service team finishes their OpenAPI rewrite.
 */
@RestController
@RequestMapping("/api/tickets")
class TicketProxyController(
    private val proxy: ProxyService,
    @Qualifier("ticketServiceUrl") private val ticketServiceUrl: String,
) {

    @GetMapping("/{id}")
    fun get(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @PostMapping
    fun create(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)

    @PostMapping("/validate")
    fun validate(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)
}
