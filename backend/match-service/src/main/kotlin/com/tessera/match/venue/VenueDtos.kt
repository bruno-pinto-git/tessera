package com.tessera.match.venue

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@JsonInclude(JsonInclude.Include.ALWAYS)
data class VenueResponse(
    val id: Long,
    val name: String,
    val capacity: Int,
    val address: String?,
    val createdAt: String,
)

data class VenueCreateRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val name: String,

    @field:NotNull
    @field:Min(0)
    @field:Max(200_000)
    val capacity: Int,

    @field:Size(max = 500)
    val address: String? = null,
)

data class VenueUpdateRequest(
    @field:Size(min = 2, max = 200)
    val name: String? = null,

    @field:Min(0)
    @field:Max(200_000)
    val capacity: Int? = null,

    @field:Size(max = 500)
    val address: String? = null,
)

internal fun Venue.toResponse() = VenueResponse(
    id = id,
    name = name,
    capacity = capacity,
    address = address,
    createdAt = createdAt.toString(),
)
