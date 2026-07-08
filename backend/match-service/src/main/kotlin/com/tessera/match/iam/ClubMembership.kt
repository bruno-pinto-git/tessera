package com.tessera.match.iam

data class ClubMembership(
    val clubId: Long,
    val role: ClubRole,
)

enum class ClubRole { MANAGER, STAFF }