package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/events")
class EventProxyController(
    private val proxy: ProxyService,
    @Qualifier("ticketServiceUrl") private val ticketServiceUrl: String,
) {

    @GetMapping
    fun list(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @GetMapping("/{id}")
    fun getOne(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @PostMapping
    fun create(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)
}
