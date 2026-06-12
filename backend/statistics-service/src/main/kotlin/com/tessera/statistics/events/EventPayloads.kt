package com.tessera.statistics.events

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * Inbound payload mirrors. We mark these `JsonIgnoreProperties(ignoreUnknown=true)`
 * so adding new optional fields on the producer side does not break the
 * consumer. Contracts live in `docs/events/async-contracts.md`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MatchSheetClosedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,

    val matchId: Long,
    val season: String,
    val matchStatus: String,
    val kickoffAt: OffsetDateTime,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val homeClubId: Long,
    val awayClubId: Long,
    val venueId: Long?,
    val homeScore: Int?,
    val awayScore: Int?,
    val refereeName: String?,

    val lineup: List<LineupEntryPayload> = emptyList(),
    val occurrences: List<OccurrencePayload> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LineupEntryPayload(
    val playerId: Long,
    val teamId: Long,
    val shirtNumber: Int?,
    val role: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OccurrencePayload(
    val occurrenceId: Long,
    val minute: Int,
    val type: String,
    val teamId: Long,
    val playerId: Long,
    val replacedPlayerId: Long?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketPaidEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val ticketId: Long,
    val eventId: Long,
    /** Nullable: an event can sell tickets without being tied to a specific match. */
    val matchId: Long?,
    /** Home club of the match (for per-club sales aggregation). */
    val homeClubId: Long? = null,
    val price: BigDecimal,
    val paymentMethod: String?,
    val paidAt: OffsetDateTime,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TicketValidatedEvent(
    val version: Int = 1,
    val occurredAt: OffsetDateTime,
    val ticketId: Long,
    val matchId: Long?,
    val validatedAt: OffsetDateTime,
    /** Keycloak subject UUID of the validator (staff/admin). */
    val validatorSub: String?,
)
