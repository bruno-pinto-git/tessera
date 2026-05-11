package com.tessera.statistics.sales

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

data class SalesSummaryResponse(
    val sold: Long,
    val validated: Long,
    val revenue: BigDecimal,
    val validationRate: BigDecimal,    // 0.000 .. 1.000
)

data class SalesByMatchResponse(
    val matchId: Long,
    val sold: Long,
    val validated: Long,
    val revenue: BigDecimal,
)

@Service
class SalesService(
    private val repo: TicketSaleRepository,
) {
    @Transactional(readOnly = true)
    fun summary(): SalesSummaryResponse {
        val sold = repo.countSold()
        val validated = repo.countValidated()
        val revenue = repo.totalRevenue()
        val rate = if (sold == 0L) BigDecimal.ZERO
        else BigDecimal(validated).divide(BigDecimal(sold), 3, RoundingMode.HALF_UP)
        return SalesSummaryResponse(sold, validated, revenue, rate)
    }

    @Transactional(readOnly = true)
    fun byMatch(matchId: Long): SalesByMatchResponse = SalesByMatchResponse(
        matchId    = matchId,
        sold       = repo.countSoldByMatch(matchId),
        validated  = repo.countValidatedByMatch(matchId),
        revenue    = repo.revenueByMatch(matchId),
    )

    @Transactional(readOnly = true)
    fun inRange(from: OffsetDateTime, to: OffsetDateTime): SalesSummaryResponse {
        require(from.isBefore(to)) { "'from' must be earlier than 'to'." }
        val sold = repo.countSoldInRange(from, to)
        val revenue = repo.revenueInRange(from, to)
        // Validation in a date range is intentionally not exposed: validation
        // happens later than payment and would skew the rate. Keep it 0 for now.
        return SalesSummaryResponse(sold, 0, revenue, BigDecimal.ZERO)
    }
}

/**
 * Sales reports are admin-only — they expose revenue and ticket counts that
 * are not for public consumption.
 */
@RestController
@RequestMapping("/api/v1/stats/sales")
class SalesController(
    private val service: SalesService,
) {
    @GetMapping("/summary")
    @PreAuthorize("hasRole('admin')")
    fun summary(): SalesSummaryResponse = service.summary()

    @GetMapping("/by-match/{matchId}")
    @PreAuthorize("hasRole('admin')")
    fun byMatch(@PathVariable matchId: Long): SalesByMatchResponse =
        service.byMatch(matchId)

    @GetMapping("/range")
    @PreAuthorize("hasRole('admin')")
    fun range(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: OffsetDateTime,
    ): SalesSummaryResponse = service.inRange(from, to)
}
