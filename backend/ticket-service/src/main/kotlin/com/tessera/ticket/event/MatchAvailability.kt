package com.tessera.ticket.event

import java.time.OffsetDateTime
import java.time.ZoneOffset

object MatchAvailability {

    const val MATCH_DURATION_HOURS = 2L

    val CLOSED_STATUSES = setOf("CANCELLED", "FINISHED", "ABANDONED")

    fun closedReason(match: MatchLookupClient.MatchView): String? {
        if (match.status != null && match.status in CLOSED_STATUSES) {
            return "the match is ${match.status}"
        }
        val kickoff = match.kickoffAt?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
        if (kickoff != null &&
            OffsetDateTime.now(ZoneOffset.UTC).isAfter(kickoff.plusHours(MATCH_DURATION_HOURS))
        ) {
            return "the match has already ended"
        }
        return null
    }
}
