package com.tessera.android.shared

enum class Capability {
    BROWSE_EVENTS,
    PURCHASE,
    VIEW_OWN_TICKETS,
    VALIDATE_TICKETS,
    MANAGE_CLUB,
    ADMIN,
}

object Permissions {

    private val byRole: Map<String, Set<Capability>> = mapOf(
        "fan" to setOf(
            Capability.BROWSE_EVENTS,
            Capability.PURCHASE,
            Capability.VIEW_OWN_TICKETS,
        ),
        "staff" to setOf(
            Capability.BROWSE_EVENTS,
            Capability.VIEW_OWN_TICKETS,
            Capability.VALIDATE_TICKETS,
        ),
        "club-manager" to setOf(
            Capability.BROWSE_EVENTS,
            Capability.VIEW_OWN_TICKETS,
            Capability.VALIDATE_TICKETS,
            Capability.MANAGE_CLUB,
        ),
        "platform-admin" to setOf(
            Capability.BROWSE_EVENTS,
            Capability.PURCHASE,
            Capability.VIEW_OWN_TICKETS,
            Capability.VALIDATE_TICKETS,
            Capability.ADMIN,
        ),
    )

    private val publicCapabilities = setOf(Capability.BROWSE_EVENTS)

    fun capabilities(roles: List<String> = AuthSession.roles): Set<Capability> =
        publicCapabilities + roles.flatMap { byRole[it] ?: emptySet() }.toSet()

    fun can(capability: Capability): Boolean =
        capability in publicCapabilities ||
            AuthSession.roles.any { capability in (byRole[it] ?: emptySet()) }
}
