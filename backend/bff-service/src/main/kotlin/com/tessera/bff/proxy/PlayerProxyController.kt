package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class PlayerProxyController(
    private val proxy: ProxyService,
    @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
    @GetMapping("/api/v1/teams/{teamId}/players")
    fun listByTeam(req: HttpServletRequest)             = proxy.forward(req, matchUrl, null)

    @GetMapping("/api/v1/clubs/{clubId}/players")
    fun listByClub(req: HttpServletRequest)             = proxy.forward(req, matchUrl, null)

    @PostMapping("/api/v1/teams/{teamId}/players")
    fun create(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

    @GetMapping("/api/v1/players/{id}")
    fun get(req: HttpServletRequest)                    = proxy.forward(req, matchUrl, null)

    @PatchMapping("/api/v1/players/{id}")
    fun update(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

    @DeleteMapping("/api/v1/players/{id}")
    fun delete(req: HttpServletRequest): ResponseEntity<String> = proxy.forward(req, matchUrl, null)
}
