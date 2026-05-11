package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Teams have two roots:
 *   - /api/v1/clubs/{clubId}/teams   (collection nested under club)
 *   - /api/v1/teams/{id}             (item)
 */
@RestController
class TeamProxyController(
    private val proxy: ProxyService,
    @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
    @GetMapping("/api/v1/clubs/{clubId}/teams")
    fun listByClub(req: HttpServletRequest)             = proxy.forward(req, matchUrl, null)

    @PostMapping("/api/v1/clubs/{clubId}/teams")
    fun create(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

    @GetMapping("/api/v1/teams/{id}")
    fun get(req: HttpServletRequest)                    = proxy.forward(req, matchUrl, null)

    @PatchMapping("/api/v1/teams/{id}")
    fun update(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

    @DeleteMapping("/api/v1/teams/{id}")
    fun delete(req: HttpServletRequest): ResponseEntity<String> = proxy.forward(req, matchUrl, null)
}
