package com.tessera.match.club

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL

@JsonInclude(JsonInclude.Include.ALWAYS)
data class ClubResponse(
    val id: Long,
    val name: String,
    val foundedYear: Int?,
    val crestUrl: String?,
    val createdAt: String,
)

data class ClubCreateRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 200)
    val name: String,

    @field:Min(1850)
    @field:Max(2100)
    val foundedYear: Int? = null,

    @field:URL
    @field:Size(max = 500)
    val crestUrl: String? = null,
)

data class ClubUpdateRequest(
    @field:Size(min = 2, max = 200)
    val name: String? = null,

    @field:Min(1850)
    @field:Max(2100)
    val foundedYear: Int? = null,

    @field:URL
    @field:Size(max = 500)
    val crestUrl: String? = null,
)

internal fun Club.toResponse() = ClubResponse(
    id = id,
    name = name,
    foundedYear = foundedYear,
    crestUrl = crestUrl,
    createdAt = createdAt.toString(),
)
