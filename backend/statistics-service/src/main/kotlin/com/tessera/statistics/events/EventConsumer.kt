package com.tessera.statistics.events

import com.tessera.statistics.matchhistory.*
import com.tessera.statistics.sales.PendingValidation
import com.tessera.statistics.sales.PendingValidationRepository
import com.tessera.statistics.sales.TicketSale
import com.tessera.statistics.sales.TicketSaleRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Consumes domain events from RabbitMQ and updates the read-side tables.
 *
 * Handlers are idempotent: re-receiving the same event reconciles to the
 * same final state. This is achieved by deleting and re-inserting the
 * dependent rows for the affected aggregate before saving.
 */
@Component
class EventConsumer(
    private val summaryRepo: MatchSummaryRepository,
    private val lineupRepo: LineupSnapshotRepository,
    private val occurrenceRepo: OccurrenceSnapshotRepository,
    private val saleRepo: TicketSaleRepository,
    private val pendingValidationRepo: PendingValidationRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = ["\${tessera.events.match-sheet-closed-queue}"])
    @Transactional
    fun onMatchSheetClosed(event: MatchSheetClosedEvent) {
        log.info("Received match.sheet.closed matchId={} status={}",
            event.matchId, event.matchStatus)

        // Idempotency: wipe any previous snapshot for this match.
        lineupRepo.deleteByIdMatchId(event.matchId)
        occurrenceRepo.deleteByMatchId(event.matchId)
        summaryRepo.deleteById(event.matchId)
        summaryRepo.flush()

        val summary = MatchSummary(
            matchId      = event.matchId,
            season       = event.season,
            matchStatus  = event.matchStatus,
            kickoffAt    = event.kickoffAt,
            homeTeamId   = event.homeTeamId,
            awayTeamId   = event.awayTeamId,
            homeClubId   = event.homeClubId,
            awayClubId   = event.awayClubId,
            venueId      = event.venueId,
            homeScore    = event.homeScore,
            awayScore    = event.awayScore,
            refereeName  = event.refereeName,
        )
        summaryRepo.save(summary)

        event.lineup.forEach {
            lineupRepo.save(LineupSnapshot(
                id           = LineupSnapshotId(event.matchId, it.playerId),
                teamId       = it.teamId,
                shirtNumber  = it.shirtNumber,
                role         = it.role,
            ))
        }
        event.occurrences.forEach {
            occurrenceRepo.save(OccurrenceSnapshot(
                occurrenceId      = it.occurrenceId,
                matchId           = event.matchId,
                minute            = it.minute,
                type              = it.type,
                teamId            = it.teamId,
                playerId          = it.playerId,
                replacedPlayerId  = it.replacedPlayerId,
            ))
        }
    }

    @RabbitListener(queues = ["\${tessera.events.match-sheet-reopened-queue}"])
    @Transactional
    fun onMatchSheetReopened(event: MatchSheetReopenedEvent) {
        log.info("Received match.sheet.reopened matchId={}", event.matchId)
        // The sheet is being edited again, so the published snapshot is no
        // longer valid. Drop it; a later match.sheet.closed rebuilds it.
        lineupRepo.deleteByIdMatchId(event.matchId)
        occurrenceRepo.deleteByMatchId(event.matchId)
        if (summaryRepo.existsById(event.matchId)) summaryRepo.deleteById(event.matchId)
    }

    @RabbitListener(queues = ["\${tessera.events.ticket-paid-queue}"])
    @Transactional
    fun onTicketPaid(event: TicketPaidEvent) {
        log.info("Received ticket.ticket.paid ticketId={} matchId={}",
            event.ticketId, event.matchId)
        // Carry over a validation that arrived before this paid event
        // (out-of-order delivery), and preserve one already stamped on a row
        // we're replacing, so we never lose a validation.
        val pending = pendingValidationRepo.findById(event.ticketId).orElse(null)
        val existingValidatedAt = saleRepo.findById(event.ticketId).orElse(null)?.validatedAt
        // Upsert via delete-or-merge: if a row already exists, replace.
        saleRepo.findById(event.ticketId).ifPresent { saleRepo.delete(it) }
        saleRepo.flush()
        saleRepo.save(TicketSale(
            ticketId       = event.ticketId,
            eventId        = event.eventId,
            matchId        = event.matchId,
            homeClubId     = event.homeClubId,
            price          = event.price,
            paymentMethod  = event.paymentMethod,
            paidAt         = event.paidAt,
            validatedAt    = existingValidatedAt ?: pending?.validatedAt,
        ))
        if (pending != null) pendingValidationRepo.delete(pending)
    }

    @RabbitListener(queues = ["\${tessera.events.ticket-validated-queue}"])
    @Transactional
    fun onTicketValidated(event: TicketValidatedEvent) {
        log.info("Received ticket.ticket.validated ticketId={}", event.ticketId)
        val sale = saleRepo.findById(event.ticketId).orElse(null)
        if (sale == null) {
            // Out-of-order delivery: the paid event hasn't landed yet. Park the
            // validation so onTicketPaid can drain it instead of losing it.
            log.warn("ticket.ticket.validated before ticket.paid for ticketId={}; parking it",
                event.ticketId)
            pendingValidationRepo.save(PendingValidation(event.ticketId, event.validatedAt))
            return
        }
        sale.validatedAt = event.validatedAt
        saleRepo.save(sale)
    }
}
