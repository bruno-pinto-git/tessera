package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/matches/{matchId}/sheet")
class MatchSheetProxyController(
  private val proxy: ProxyService,
  @Qualifier("matchServiceUrl") private val matchUrl: String,
) {
  @GetMapping
  fun get(req: HttpServletRequest) = proxy.forward(req, matchUrl, null)

  @PostMapping("/lineup")
  fun addLineup(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

  @PatchMapping("/lineup/{playerId}")
  fun updateLineup(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

  @DeleteMapping("/lineup/{playerId}")
  fun removeLineup(req: HttpServletRequest) = proxy.forward(req, matchUrl, null)

  @PostMapping("/occurrences")
  fun addOccurrence(@RequestBody b: String, r: HttpServletRequest) = proxy.forward(r, matchUrl, b)

  @DeleteMapping("/occurrences/{occId}")
  fun removeOccurrence(req: HttpServletRequest) = proxy.forward(req, matchUrl, null)

  @PostMapping("/lock")
  fun lock(req: HttpServletRequest) = proxy.forward(req, matchUrl, null)

  @PostMapping("/unlock")
  fun unlock(req: HttpServletRequest): ResponseEntity<String> = proxy.forward(req, matchUrl, null)
}
