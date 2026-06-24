package com.tessera.android.data.dto

data class ClubMembershipDto(
    val clubId: Long,
    val role: String,
)

data class MeDto(
    val sub: String,
    val username: String?,
    val roles: List<String>,
    val clubMemberships: List<ClubMembershipDto>,
)

data class ClubDto(
    val id: Long,
    val name: String,
    val foundedYear: Int?,
    val crestUrl: String?,
)

data class MemberDto(
    val userId: String,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val role: String,
)

data class MembersDto(
    val managers: List<MemberDto>,
    val staff: List<MemberDto>,
)

data class TeamDto(
    val id: Long,
    val clubId: Long,
    val category: String,
)

data class PlayerDto(
    val id: Long,
    val teamId: Long,
    val firstName: String,
    val lastName: String,
    val position: String,
    val shirtNumber: Int?,
    val status: String,
    val birthdate: String? = null,
    val nationality: String? = null,
    val dominantFoot: String? = null,
    val height: Int? = null,
    val weight: Int? = null,
    val photoUrl: String? = null,
)

data class PlayerInput(
    val firstName: String,
    val lastName: String,
    val position: String,
    val status: String,
    val shirtNumber: Int?,
    val birthdate: String?,
    val nationality: String?,
    val dominantFoot: String?,
    val height: Int?,
    val weight: Int?,
    val photoUrl: String?,
)

data class UserDto(
    val id: String,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val enabled: Boolean,
    val roles: List<String>,
)

data class VenueDto(
    val id: Long,
    val name: String,
    val capacity: Int,
    val address: String?,
)

data class MatchDto(
    val id: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeClubId: Long?,
    val awayClubId: Long?,
    val venueId: Long?,
    val kickoffAt: String,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val refereeName: String?,
)
