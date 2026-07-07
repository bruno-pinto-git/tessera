package com.tessera.statistics.sales

import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
    val validationRate: BigDecimal,
)

data class SalesByMatchResponse(
    val matchId: Long,
    val sold: Long,
    val validated: Long,
    val revenue: BigDecimal,
)

data class SalesByClubResponse(
    val clubId: Long,
    val sold: Long,
    val validated: Long,
    val revenue: BigDecimal?,
    val validationRate: BigDecimal,
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
    fun byClub(clubId: Long): SalesByClubResponse {
        val sold = repo.countSoldByClub(clubId)
        val validated = repo.countValidatedByClub(clubId)
        val revenue = repo.revenueByClub(clubId)
        val rate = if (sold == 0L) BigDecimal.ZERO
        else BigDecimal(validated).divide(BigDecimal(sold), 3, RoundingMode.HALF_UP)
        return SalesByClubResponse(clubId, sold, validated, revenue, rate)
    }

    @Transactional(readOnly = true)
    fun inRange(from: OffsetDateTime, to: OffsetDateTime): SalesSummaryResponse {
        require(from.isBefore(to)) { "'from' must be earlier than 'to'." }
        val sold = repo.countSoldInRange(from, to)
        val revenue = repo.revenueInRange(from, to)
        return SalesSummaryResponse(sold, 0, revenue, BigDecimal.ZERO)
    }
}

@RestController
@RequestMapping("/api/v1/stats/sales")
class SalesController(
    private val service: SalesService,
) {
    @GetMapping("/summary")
    @PreAuthorize("hasRole('platform-admin')")
    fun summary(): SalesSummaryResponse = service.summary()

    @GetMapping("/by-match/{matchId}")
    @PreAuthorize("hasRole('platform-admin')")
    fun byMatch(@PathVariable matchId: Long): SalesByMatchResponse =
        service.byMatch(matchId)

    @GetMapping("/range")
    @PreAuthorize("hasRole('platform-admin')")
    fun range(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: OffsetDateTime,
    ): SalesSummaryResponse = service.inRange(from, to)

    @GetMapping("/by-club/{clubId}")
    @PreAuthorize("isAuthenticated()")
    fun byClub(
        @PathVariable clubId: Long,
        @AuthenticationPrincipal jwt: Jwt,
    ): SalesByClubResponse {
        val isAdmin = jwt.isPlatformAdmin()
        val isManager = clubId in jwt.managedClubIds()
        val isStaff = clubId in jwt.staffClubIds()
        if (!isAdmin && !isManager && !isStaff) {
            throw AccessDeniedException("Not allowed to read sales for club $clubId.")
        }
        val result = service.byClub(clubId)
        return if (isAdmin || isManager) result else result.copy(revenue = null)
    }
}
