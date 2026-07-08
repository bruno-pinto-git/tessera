package com.tessera.match.events

import java.time.OffsetDateTime

data class MatchSheetClosedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,

    val matchId: Long,
    val season: String,
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
    val role: String,
)

data class OccurrencePayload(
    val occurrenceId: Long,
    val minute: Int,
    val type: String,
    val teamId: Long,
    val playerId: Long,
    val replacedPlayerId: Long?,
)

data class MatchSheetReopenedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val matchId: Long,
)
