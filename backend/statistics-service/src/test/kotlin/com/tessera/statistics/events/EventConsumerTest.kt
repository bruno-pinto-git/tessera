package com.tessera.statistics.events

import com.tessera.statistics.matchhistory.LineupSnapshotRepository
import com.tessera.statistics.matchhistory.MatchSummaryRepository
import com.tessera.statistics.matchhistory.OccurrenceSnapshotRepository
import com.tessera.statistics.sales.TicketSale
import com.tessera.statistics.sales.TicketSaleRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.assertEquals

/**
 * Unit tests for [EventConsumer] — the read-side projection built from RabbitMQ
 * events. Focus on idempotency (delete-then-reinsert) and out-of-order
 * resilience (a validated event before its paid event is dropped). Repositories
 * are mocked; no broker or DB is involved.
 */
class EventConsumerTest {

    private val summaryRepo: MatchSummaryRepository = mock()
    private val lineupRepo: LineupSnapshotRepository = mock()
    private val occurrenceRepo: OccurrenceSnapshotRepository = mock()
    private val saleRepo: TicketSaleRepository = mock()

    private val consumer = EventConsumer(summaryRepo, lineupRepo, occurrenceRepo, saleRepo)

    @Test
    fun `match sheet closed wipes prior snapshots then re-inserts`() {
        consumer.onMatchSheetClosed(matchSheetClosed())

        // Idempotency: prior rows are deleted before re-insertion.
        verify(lineupRepo).deleteByIdMatchId(5L)
        verify(occurrenceRepo).deleteByMatchId(5L)
        verify(summaryRepo).deleteById(5L)
        // Re-inserted.
        verify(summaryRepo).save(any())
        verify(lineupRepo).save(any())
        verify(occurrenceRepo).save(any())
    }

    @Test
    fun `ticket paid inserts a fresh sale`() {
        whenever(saleRepo.findById(7L)).thenReturn(Optional.empty())

        consumer.onTicketPaid(ticketPaid())

        verify(saleRepo, never()).delete(any())
        verify(saleRepo).save(any())
    }

    @Test
    fun `ticket paid replaces an existing sale (idempotent upsert)`() {
        val existing = sale()
        whenever(saleRepo.findById(7L)).thenReturn(Optional.of(existing))

        consumer.onTicketPaid(ticketPaid())

        verify(saleRepo).delete(existing)
        verify(saleRepo).save(any())
    }

    @Test
    fun `ticket validated stamps an existing sale`() {
        val existing = sale()
        whenever(saleRepo.findById(7L)).thenReturn(Optional.of(existing))
        val at = OffsetDateTime.parse("2026-05-01T20:00:00Z")

        consumer.onTicketValidated(ticketValidated(at))

        assertEquals(at, existing.validatedAt)
        verify(saleRepo).save(existing)
    }

    @Test
    fun `ticket validated before paid is dropped (out-of-order)`() {
        whenever(saleRepo.findById(7L)).thenReturn(Optional.empty())

        consumer.onTicketValidated(ticketValidated(OffsetDateTime.parse("2026-05-01T20:00:00Z")))

        verify(saleRepo, never()).save(any())
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun matchSheetClosed() = MatchSheetClosedEvent(
        occurredAt = OffsetDateTime.parse("2026-05-01T22:00:00Z"),
        matchId = 5L,
        season = "2025/2026",
        matchStatus = "FINISHED",
        kickoffAt = OffsetDateTime.parse("2026-05-01T20:00:00Z"),
        homeTeamId = 1L,
        awayTeamId = 2L,
        homeClubId = 10L,
        awayClubId = 20L,
        venueId = 3L,
        homeScore = 2,
        awayScore = 1,
        refereeName = "Ref",
        lineup = listOf(LineupEntryPayload(playerId = 1L, teamId = 1L, shirtNumber = 7, role = "STARTER")),
        occurrences = listOf(
            OccurrencePayload(
                occurrenceId = 1L, minute = 10, type = "GOAL",
                teamId = 1L, playerId = 1L, replacedPlayerId = null,
            ),
        ),
    )

    private fun ticketPaid() = TicketPaidEvent(
        occurredAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
        ticketId = 7L,
        eventId = 1L,
        matchId = 5L,
        price = BigDecimal("10.00"),
        paymentMethod = "CARD",
        paidAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
    )

    private fun ticketValidated(at: OffsetDateTime) = TicketValidatedEvent(
        occurredAt = at,
        ticketId = 7L,
        matchId = 5L,
        validatedAt = at,
        validatorSub = "staff-sub",
    )

    private fun sale() = TicketSale(
        ticketId = 7L,
        eventId = 1L,
        matchId = 5L,
        price = BigDecimal("10.00"),
        paymentMethod = "CARD",
        paidAt = OffsetDateTime.parse("2026-05-01T19:00:00Z"),
    )
}
