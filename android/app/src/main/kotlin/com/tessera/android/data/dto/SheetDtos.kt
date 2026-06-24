package com.tessera.android.data.dto

data class LineupEntryDto(
    val playerId: Long,
    val teamId: Long,
    val role: String,
    val shirtNumber: Int?,
)

data class OccurrenceDto(
    val id: Long,
    val minute: Int,
    val type: String,
    val playerId: Long,
    val teamId: Long?,
    val replacedPlayerId: Long?,
)

data class MatchSheetDto(
    val matchId: Long,
    val locked: Boolean,
    val lineup: List<LineupEntryDto>,
    val occurrences: List<OccurrenceDto>,
)
