package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tickets")
class TicketProxyController(
    private val proxy: ProxyService,
    @Qualifier("ticketServiceUrl") private val ticketServiceUrl: String,
) {

    @GetMapping
    fun listByEvent(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @GetMapping("/mine")
    fun listMine(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @GetMapping("/{id}")
    fun getOne(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body = null)

    @PostMapping
    fun create(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)

    @PostMapping("/{id}/pay")
    fun pay(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)

    @PostMapping("/{id}/wallet-pass")
    fun walletPass(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)

    @PostMapping("/validate")
    fun validate(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, ticketServiceUrl, body)
}
