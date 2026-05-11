package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/matches")
class MatchProxyController(
    private val proxy: ProxyService,
    @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
    @GetMapping fun list(req: HttpServletRequest)               = proxy.forward(req, matchUrl, null)
    @GetMapping("/{id}") fun get(req: HttpServletRequest)       = proxy.forward(req, matchUrl, null)
    @PostMapping fun create(@RequestBody b: String, r: HttpServletRequest)         = proxy.forward(r, matchUrl, b)
    @PatchMapping("/{id}") fun update(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)
    @DeleteMapping("/{id}") fun delete(req: HttpServletRequest): ResponseEntity<String> = proxy.forward(req, matchUrl, null)
}
