package com.tessera.statistics.events

import com.tessera.statistics.matchhistory.*
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

    @RabbitListener(queues = ["\${tessera.events.ticket-paid-queue}"])
    @Transactional
    fun onTicketPaid(event: TicketPaidEvent) {
        log.info("Received ticket.ticket.paid ticketId={} matchId={}",
            event.ticketId, event.matchId)
        // Upsert via delete-or-merge: if a row already exists, replace.
        saleRepo.findById(event.ticketId).ifPresent { saleRepo.delete(it) }
        saleRepo.flush()
        saleRepo.save(TicketSale(
            ticketId       = event.ticketId,
            eventId        = event.eventId,
            matchId        = event.matchId,
            price          = event.price,
            paymentMethod  = event.paymentMethod,
            paidAt         = event.paidAt,
        ))
    }

    @RabbitListener(queues = ["\${tessera.events.ticket-validated-queue}"])
    @Transactional
    fun onTicketValidated(event: TicketValidatedEvent) {
        log.info("Received ticket.ticket.validated ticketId={}", event.ticketId)
        // We can only stamp a validated_at if we already saw the paid event.
        // If not (out-of-order delivery), we log and drop — the consumer will
        // pick it up next time the producer re-emits both events.
        val sale = saleRepo.findById(event.ticketId).orElse(null)
        if (sale == null) {
            log.warn("ticket.ticket.validated received before ticket.paid for ticketId={}",
                event.ticketId)
            return
        }
        sale.validatedAt = event.validatedAt
        saleRepo.save(sale)
    }
}
