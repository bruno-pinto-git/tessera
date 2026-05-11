package com.tessera.statistics.sales

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime

interface TicketSaleRepository : JpaRepository<TicketSale, Long> {

    @Query("SELECT COUNT(t) FROM TicketSale t")
    fun countSold(): Long

    @Query("SELECT COUNT(t) FROM TicketSale t WHERE t.validatedAt IS NOT NULL")
    fun countValidated(): Long

    @Query("SELECT COALESCE(SUM(t.price), 0) FROM TicketSale t")
    fun totalRevenue(): BigDecimal

    @Query("SELECT COUNT(t) FROM TicketSale t WHERE t.matchId = :matchId")
    fun countSoldByMatch(@Param("matchId") matchId: Long): Long

    @Query("SELECT COUNT(t) FROM TicketSale t WHERE t.matchId = :matchId AND t.validatedAt IS NOT NULL")
    fun countValidatedByMatch(@Param("matchId") matchId: Long): Long

    @Query("SELECT COALESCE(SUM(t.price), 0) FROM TicketSale t WHERE t.matchId = :matchId")
    fun revenueByMatch(@Param("matchId") matchId: Long): BigDecimal

    @Query("""
        SELECT COUNT(t) FROM TicketSale t
         WHERE t.paidAt >= :from AND t.paidAt < :to
    """)
    fun countSoldInRange(
        @Param("from") from: OffsetDateTime,
        @Param("to")   to: OffsetDateTime,
    ): Long

    @Query("""
        SELECT COALESCE(SUM(t.price), 0) FROM TicketSale t
         WHERE t.paidAt >= :from AND t.paidAt < :to
    """)
    fun revenueInRange(
        @Param("from") from: OffsetDateTime,
        @Param("to")   to: OffsetDateTime,
    ): BigDecimal
}
