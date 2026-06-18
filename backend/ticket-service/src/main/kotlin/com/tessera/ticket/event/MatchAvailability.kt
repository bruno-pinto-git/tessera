package com.tessera.ticket.event

import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Single source of truth for "is this match still open for business?" — used
 * both when opening a box office (EventController) and when selling a ticket
 * (TicketService). A match is closed once it's cancelled/finished/abandoned or
 * past kickoff + the assumed match duration.
 */
object MatchAvailability {

    /** A match (incl. half-time and stoppage) is treated as lasting at most this long. */
    const val MATCH_DURATION_HOURS = 2L

    /** Match states in which no box office opens and no tickets sell. */
    val CLOSED_STATUSES = setOf("CANCELLED", "FINISHED", "ABANDONED")

    /**
     * A human-readable reason the match is no longer open for sales, or null if
     * it's still open (or can't be evaluated — callers decide whether to block).
     */
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
