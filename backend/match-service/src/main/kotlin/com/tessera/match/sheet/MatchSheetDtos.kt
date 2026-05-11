package com.tessera.match.sheet

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.ALWAYS)
data class LineupEntryResponse(
    val playerId: Long,
    val teamId: Long,
    val shirtNumber: Int?,
    val role: LineupRole,
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class MatchSheetResponse(
    val matchId: Long,
    val locked: Boolean,
    val lockedAt: String?,
    val lineup: List<LineupEntryResponse>,
    val occurrences: List<OccurrenceResponse>,
)

@JsonInclude(JsonInclude.Include.ALWAYS)
data class OccurrenceResponse(
    val id: Long,
    val minute: Int,
    val type: OccurrenceType,
    val teamId: Long,
    val playerId: Long,
    val replacedPlayerId: Long?,
    val createdAt: String,
)

data class OccurrenceCreateRequest(
    @field:jakarta.validation.constraints.Min(0)
    @field:jakarta.validation.constraints.Max(200)
    @field:jakarta.validation.constraints.NotNull
    val minute: Int,

    @field:jakarta.validation.constraints.NotNull
    val type: OccurrenceType,

    @field:jakarta.validation.constraints.NotNull
    val playerId: Long,

    val replacedPlayerId: Long? = null,
)

internal fun Occurrence.toResponse() = OccurrenceResponse(
    id = id,
    minute = minute,
    type = type,
    teamId = teamId,
    playerId = playerId,
    replacedPlayerId = replacedPlayerId,
    createdAt = createdAt.toString(),
)

data class LineupCreateRequest(
    @field:NotNull
    val playerId: Long,

    @field:Min(1) @field:Max(99)
    val shirtNumber: Int? = null,

    @field:NotNull
    val role: LineupRole,
)

data class LineupUpdateRequest(
    @field:Min(1) @field:Max(99)
    val shirtNumber: Int? = null,

    val role: LineupRole? = null,
)

internal fun LineupEntry.toResponse() = LineupEntryResponse(
    playerId = id.playerId,
    teamId = teamId,
    shirtNumber = shirtNumber,
    role = role,
)
