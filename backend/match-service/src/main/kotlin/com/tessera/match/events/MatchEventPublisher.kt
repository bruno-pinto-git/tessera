package com.tessera.match.events

import com.tessera.match.match.MatchRepository
import com.tessera.match.sheet.LineupEntryRepository
import com.tessera.match.sheet.MatchSheet
import com.tessera.match.sheet.OccurrenceRepository
import com.tessera.match.team.TeamRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class MatchEventPublisher(
    private val rabbit: RabbitTemplate,
    private val matchRepo: MatchRepository,
    private val teamRepo: TeamRepository,
    private val lineupRepo: LineupEntryRepository,
    private val occurrenceRepo: OccurrenceRepository,
    @Value("\${tessera.events.exchange}") private val exchange: String,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publishMatchSheetClosed(sheet: MatchSheet) {
        val match = matchRepo.findById(sheet.matchId).orElse(null) ?: run {
            log.warn("Cannot publish MatchSheetClosed: match {} not found", sheet.matchId)
            return
        }
        val homeTeam = teamRepo.findById(match.homeTeamId).orElse(null)
        val awayTeam = teamRepo.findById(match.awayTeamId).orElse(null)
        if (homeTeam == null || awayTeam == null) {
            log.warn("Cannot publish MatchSheetClosed: missing teams for match {}", match.id)
            return
        }

        val lineup = lineupRepo.findBySheet(sheet.id).map {
            LineupEntryPayload(
                playerId      = it.id.playerId,
                teamId        = it.teamId,
                shirtNumber   = it.shirtNumber,
                role          = it.role.name,
            )
        }
        val occurrences = occurrenceRepo.findBySheet(sheet.id).map {
            OccurrencePayload(
                occurrenceId     = it.id,
                minute           = it.minute,
                type             = it.type.name,
                teamId           = it.teamId,
                playerId         = it.playerId,
                replacedPlayerId = it.replacedPlayerId,
            )
        }

        val event = MatchSheetClosedEvent(
            occurredAt   = OffsetDateTime.now(ZoneOffset.UTC),
            matchId      = match.id,
            season       = seasonOf(match.kickoffAt),
            matchStatus  = match.status.name,
            kickoffAt    = match.kickoffAt,
            homeTeamId   = match.homeTeamId,
            awayTeamId   = match.awayTeamId,
            homeClubId   = homeTeam.clubId,
            awayClubId   = awayTeam.clubId,
            venueId      = match.venueId,
            homeScore    = match.homeScore,
            awayScore    = match.awayScore,
            refereeName  = match.refereeName,
            lineup       = lineup,
            occurrences  = occurrences,
        )

        try {
            rabbit.convertAndSend(exchange, ROUTING_SHEET_CLOSED, event)
            log.info("Published match.sheet.closed for matchId={}", match.id)
        } catch (e: Exception) {
            log.error("Failed to publish match.sheet.closed for matchId={}", match.id, e)
        }
    }

    fun publishMatchSheetReopened(matchId: Long) {
        val event = MatchSheetReopenedEvent(
            occurredAt = OffsetDateTime.now(ZoneOffset.UTC),
            matchId    = matchId,
        )
        try {
            rabbit.convertAndSend(exchange, ROUTING_SHEET_REOPENED, event)
            log.info("Published match.sheet.reopened for matchId={}", matchId)
        } catch (e: Exception) {
            log.error("Failed to publish match.sheet.reopened for matchId={}", matchId, e)
        }
    }

    companion object {
        const val ROUTING_SHEET_CLOSED = "match.sheet.closed"
        const val ROUTING_SHEET_REOPENED = "match.sheet.reopened"

        internal fun seasonOf(kickoffAt: OffsetDateTime): String {
            val date = kickoffAt.toLocalDate()
            val startYear = if (date.month >= Month.JULY) date.year else date.year - 1
            val endYearShort = (startYear + 1) % 100
            return "%d-%02d".format(startYear, endYearShort)
        }
    }
}
