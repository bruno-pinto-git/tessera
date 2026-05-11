package com.tessera.match.events

import java.time.OffsetDateTime

/**
 * Event payload for `match.sheet.closed`.
 *
 * Contract spec: docs/events/async-contracts.md
 * Fired by MatchSheetService when a sheet is locked (either manually or by
 * auto-lock when the parent match enters a terminal status).
 *
 * Carries everything the statistics-service needs to build its read-side
 * snapshot, avoiding any follow-up HTTP calls to the match-service.
 */
data class MatchSheetClosedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,

    val matchId: Long,
    val season: String,             // e.g. "2026-27"
    val matchStatus: String,
    val kickoffAt: OffsetDateTime,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeClubId: Long,
    val awayClubId: Long,
    val venueId: Long?,
    val homeScore: Int?,
    val awayScore: Int?,
    val refereeName: String?,

    val lineup: List<LineupEntryPayload>,
    val occurrences: List<OccurrencePayload>,
)

data class LineupEntryPayload(
    val playerId: Long,
    val teamId: Long,
    val shirtNumber: Int?,
    val role: String,               // STARTER | SUBSTITUTE
)

data class OccurrencePayload(
    val occurrenceId: Long,
    val minute: Int,
    val type: String,               // GOAL | OWN_GOAL | YELLOW_CARD | RED_CARD | SUBSTITUTION | FOUL
    val teamId: Long,
    val playerId: Long,
    val replacedPlayerId: Long?,
)
