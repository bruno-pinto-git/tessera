package com.tessera.match.match

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.ALWAYS)
data class MatchResponse(
    val id: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    /** Club of the home/away team. Null if the team can't be resolved. */
    val homeClubId: Long?,
    val awayClubId: Long?,
    val venueId: Long?,
    val kickoffAt: String,
    val status: MatchStatus,
    val homeScore: Int?,
    val awayScore: Int?,
    val refereeName: String?,
    val createdAt: String,
)

data class MatchCreateRequest(
    @field:NotNull val homeTeamId: Long,
    @field:NotNull val awayTeamId: Long,
    val venueId: Long? = null,
    @field:NotNull val kickoffAt: OffsetDateTime,
    @field:Size(max = 200) val refereeName: String? = null,
)

data class MatchUpdateRequest(
    val venueId: Long? = null,
    val kickoffAt: OffsetDateTime? = null,
    val status: MatchStatus? = null,
    @field:Min(0) val homeScore: Int? = null,
    @field:Min(0) val awayScore: Int? = null,
    @field:Size(max = 200) val refereeName: String? = null,
)

internal fun Match.toResponse(homeClubId: Long? = null, awayClubId: Long? = null) = MatchResponse(
    id = id,
    homeTeamId = homeTeamId,
    awayTeamId = awayTeamId,
    homeClubId = homeClubId,
    awayClubId = awayClubId,
    venueId = venueId,
    kickoffAt = kickoffAt.toString(),
    status = status,
    homeScore = homeScore,
    awayScore = awayScore,
    refereeName = refereeName,
    createdAt = createdAt.toString(),
)
