package com.tessera.bff.proxy

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/stats")
class StatisticsProxyController(
    private val proxy: ProxyService,
    @Qualifier("statisticsServiceUrl") private val statsUrl: String,
) {
    // match-sheets history
    @GetMapping("/match-sheets")
    fun listSheets(req: HttpServletRequest) = proxy.forward(req, statsUrl, null)

    @GetMapping("/match-sheets/{matchId}")
    fun getSheet(req: HttpServletRequest) = proxy.forward(req, statsUrl, null)

    // sales
    @GetMapping("/sales/summary")
    fun salesSummary(req: HttpServletRequest) = proxy.forward(req, statsUrl, null)

    @GetMapping("/sales/by-match/{matchId}")
    fun salesByMatch(req: HttpServletRequest) = proxy.forward(req, statsUrl, null)

    @GetMapping("/sales/by-club/{clubId}")
    fun salesByClub(req: HttpServletRequest) = proxy.forward(req, statsUrl, null)

    @GetMapping("/sales/range")
    fun salesRange(req: HttpServletRequest): ResponseEntity<String> = proxy.forward(req, statsUrl, null)
}
