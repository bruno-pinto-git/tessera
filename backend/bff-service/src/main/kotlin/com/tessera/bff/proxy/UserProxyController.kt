package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users")
class UserProxyController(
    private val proxy: ProxyService,
    @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
    @GetMapping
    fun search(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, null)

    @GetMapping("/{id}")
    fun get(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, null)

    @PostMapping
    fun create(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, body)

    @PutMapping("/{id}")
    fun update(@RequestBody body: String, req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, body)

    @DeleteMapping("/{id}")
    fun delete(req: HttpServletRequest): ResponseEntity<String> =
        proxy.forward(req, matchUrl, null)
}