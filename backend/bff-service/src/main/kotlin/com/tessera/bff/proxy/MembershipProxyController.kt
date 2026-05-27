package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/clubs/{clubId}/members")
class MembershipProxyController(
    private val proxy: ProxyService,
    @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
    @GetMapping
    fun list(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, null)

    @PostMapping
    fun add(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, body)

    @DeleteMapping("/{userId}")
    fun remove(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, null)
}