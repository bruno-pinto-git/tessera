package com.tessera.match.team

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.NotNull

@JsonInclude(JsonInclude.Include.ALWAYS)
data class TeamResponse(
    val id: Long,
    val clubId: Long,
    val category: TeamCategory,
    val createdAt: String,
)

data class TeamCreateRequest(
    @field:NotNull
    val category: TeamCategory,
)

data class TeamUpdateRequest(
    val category: TeamCategory? = null,
)

internal fun Team.toResponse() = TeamResponse(
    id = id,
    clubId = clubId,
    category = category,
    createdAt = createdAt.toString(),
)
