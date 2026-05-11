package com.tessera.match.player

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.ALWAYS)
data class PlayerResponse(
    val id: Long,
    val teamId: Long,
    val firstName: String,
    val lastName: String,
    val birthdate: String?,
    val nationality: String?,
    val position: PlayerPosition,
    val shirtNumber: Int?,
    val photoUrl: String?,
    val dominantFoot: DominantFoot?,
    val height: Int?,
    val weight: Int?,
    val status: PlayerStatus,
    val createdAt: String,
)

data class PlayerCreateRequest(
    @field:NotBlank @field:Size(min = 1, max = 100)
    val firstName: String,

    @field:NotBlank @field:Size(min = 1, max = 100)
    val lastName: String,

    val birthdate: LocalDate? = null,

    @field:Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 3166-1 alpha-3 code")
    val nationality: String? = null,

    @field:NotNull
    val position: PlayerPosition,

    @field:Min(1) @field:Max(99)
    val shirtNumber: Int? = null,

    @field:URL @field:Size(max = 500)
    val photoUrl: String? = null,

    val dominantFoot: DominantFoot? = null,

    @field:Min(100) @field:Max(250)
    val height: Int? = null,

    @field:Min(30) @field:Max(200)
    val weight: Int? = null,

    val status: PlayerStatus? = null,
)

data class PlayerUpdateRequest(
    @field:Size(min = 1, max = 100)
    val firstName: String? = null,

    @field:Size(min = 1, max = 100)
    val lastName: String? = null,

    val birthdate: LocalDate? = null,

    @field:Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 3166-1 alpha-3 code")
    val nationality: String? = null,

    val position: PlayerPosition? = null,

    @field:Min(1) @field:Max(99)
    val shirtNumber: Int? = null,

    @field:URL @field:Size(max = 500)
    val photoUrl: String? = null,

    val dominantFoot: DominantFoot? = null,

    @field:Min(100) @field:Max(250)
    val height: Int? = null,

    @field:Min(30) @field:Max(200)
    val weight: Int? = null,

    val status: PlayerStatus? = null,
)

internal fun Player.toResponse() = PlayerResponse(
    id = id,
    teamId = teamId,
    firstName = firstName,
    lastName = lastName,
    birthdate = birthdate?.toString(),
    nationality = nationality,
    position = position,
    shirtNumber = shirtNumber,
    photoUrl = photoUrl,
    dominantFoot = dominantFoot,
    height = height,
    weight = weight,
    status = status,
    createdAt = createdAt.toString(),
)
