package com.tessera.statistics.matchhistory

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.ALWAYS)
data class MatchSummaryResponse(
    val matchId: Long,
    val season: String,
    val matchStatus: String,
    val kickoffAt: String,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeClubId: Long,
    val awayClubId: Long,
    val venueId: Long?,
    val homeScore: Int?,
    val awayScore: Int?,
    val refereeName: String?,
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class LineupEntryResponse(
    val playerId: Long,
    val teamId: Long,
    val shirtNumber: Int?,
    val role: String,
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class OccurrenceResponse(
    val occurrenceId: Long,
    val minute: Int,
    val type: String,
    val teamId: Long,
    val playerId: Long,
    val replacedPlayerId: Long?,
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class MatchSheetHistoryResponse(
    val matchId: Long,
    val summary: MatchSummaryResponse,
    val lineup: List<LineupEntryResponse>,
    val occurrences: List<OccurrenceResponse>,
)

internal fun MatchSummary.toResponse() = MatchSummaryResponse(
    matchId      = matchId,
    season       = season,
    matchStatus  = matchStatus,
    kickoffAt    = kickoffAt.toString(),
    homeTeamId   = homeTeamId,
    awayTeamId   = awayTeamId,
    homeClubId   = homeClubId,
    awayClubId   = awayClubId,
    venueId      = venueId,
    homeScore    = homeScore,
    awayScore    = awayScore,
    refereeName  = refereeName,
)

internal fun LineupSnapshot.toResponse() = LineupEntryResponse(
    playerId     = id.playerId,
    teamId       = teamId,
    shirtNumber  = shirtNumber,
    role         = role,
)

internal fun OccurrenceSnapshot.toResponse() = OccurrenceResponse(
    occurrenceId     = occurrenceId,
    minute           = minute,
    type             = type,
    teamId           = teamId,
    playerId         = playerId,
    replacedPlayerId = replacedPlayerId,
)
