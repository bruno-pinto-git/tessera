package com.tessera.statistics.sales

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [SalesService] aggregation math — the validation-rate
 * calculation (with its zero-division guard) and the date-range guard.
 * Mirrors docs/http-tests/08-statistics.http (sales read-side).
 */
class SalesServiceTest {

    private val repo: TicketSaleRepository = mock()
    private val service = SalesService(repo)

    @Test
    fun `summary computes the validation rate`() {
        whenever(repo.countSold()).thenReturn(10L)
        whenever(repo.countValidated()).thenReturn(4L)
        doReturn(BigDecimal("100.00")).whenever(repo).totalRevenue()

        val s = service.summary()

        assertEquals(10L, s.sold)
        assertEquals(4L, s.validated)
        assertEquals(BigDecimal("100.00"), s.revenue)
        assertEquals(BigDecimal("0.400"), s.validationRate)
    }

    @Test
    fun `summary guards against division by zero when nothing is sold`() {
        whenever(repo.countSold()).thenReturn(0L)
        whenever(repo.countValidated()).thenReturn(0L)
        doReturn(BigDecimal.ZERO).whenever(repo).totalRevenue()

        val s = service.summary()

        assertEquals(BigDecimal.ZERO, s.validationRate)
    }

    @Test
    fun `byMatch aggregates per match`() {
        whenever(repo.countSoldByMatch(99L)).thenReturn(3L)
        whenever(repo.countValidatedByMatch(99L)).thenReturn(1L)
        doReturn(BigDecimal("30.00")).whenever(repo).revenueByMatch(99L)

        val s = service.byMatch(99L)

        assertEquals(99L, s.matchId)
        assertEquals(3L, s.sold)
        assertEquals(1L, s.validated)
        assertEquals(BigDecimal("30.00"), s.revenue)
    }

    @Test
    fun `byClub aggregates per club and computes the validation rate`() {
        whenever(repo.countSoldByClub(5L)).thenReturn(8L)
        whenever(repo.countValidatedByClub(5L)).thenReturn(6L)
        doReturn(BigDecimal("80.00")).whenever(repo).revenueByClub(5L)

        val s = service.byClub(5L)

        assertEquals(5L, s.clubId)
        assertEquals(8L, s.sold)
        assertEquals(6L, s.validated)
        assertEquals(BigDecimal("80.00"), s.revenue)
        assertEquals(BigDecimal("0.750"), s.validationRate)
    }

    @Test
    fun `byClub guards against division by zero when the club sold nothing`() {
        whenever(repo.countSoldByClub(7L)).thenReturn(0L)
        whenever(repo.countValidatedByClub(7L)).thenReturn(0L)
        doReturn(BigDecimal.ZERO).whenever(repo).revenueByClub(7L)

        val s = service.byClub(7L)

        assertEquals(0L, s.sold)
        assertEquals(BigDecimal.ZERO, s.validationRate)
    }

    @Test
    fun `inRange returns sold and revenue and omits validation`() {
        val from = OffsetDateTime.parse("2026-01-01T00:00:00Z")
        val to = from.plusDays(7)
        whenever(repo.countSoldInRange(from, to)).thenReturn(7L)
        doReturn(BigDecimal("70.00")).whenever(repo).revenueInRange(from, to)

        val s = service.inRange(from, to)

        assertEquals(7L, s.sold)
        assertEquals(0L, s.validated)
        assertEquals(BigDecimal("70.00"), s.revenue)
        assertEquals(BigDecimal.ZERO, s.validationRate)
    }

    @Test
    fun `inRange rejects a from that is not before to`() {
        val from = OffsetDateTime.parse("2026-01-08T00:00:00Z")
        val to = from.minusDays(1)
        assertFailsWith<IllegalArgumentException> { service.inRange(from, to) }
    }
}
