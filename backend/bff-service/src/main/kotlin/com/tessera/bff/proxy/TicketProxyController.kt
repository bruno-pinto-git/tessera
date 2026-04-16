package com.tessera.bff.proxy

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate

@RestController
@RequestMapping("/api/tickets")
class TicketProxyController(
    private val restTemplate: RestTemplate,
    @Qualifier("ticketServiceUrl") private val ticketServiceUrl: String
) {

    @PostMapping
    fun createTicket(@RequestBody body: String): ResponseEntity<String> {
        return forward(HttpMethod.POST, "/api/tickets", body)
    }

    @PostMapping("/validate")
    fun validateTicket(@RequestBody body: String): ResponseEntity<String> {
        return forward(HttpMethod.POST, "/api/tickets/validate", body)
    }

    @GetMapping("/{id}")
    fun getTicket(@PathVariable id: Long): ResponseEntity<String> {
        return forward(HttpMethod.GET, "/api/tickets/$id", null)
    }

    private fun forward(method: HttpMethod, path: String, body: String?): ResponseEntity<String> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val request = HttpEntity(body, headers)

        return try {
            restTemplate.exchange(
                "$ticketServiceUrl$path",
                method,
                request,
                String::class.java
            )
        } catch (e: HttpClientErrorException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        } catch (e: HttpServerErrorException) {
            ResponseEntity.status(e.statusCode).body(e.responseBodyAsString)
        }
    }
}
