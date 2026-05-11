package com.tessera.match.sheet

import jakarta.persistence.*
import java.time.OffsetDateTime

enum class OccurrenceType {
    GOAL, OWN_GOAL, YELLOW_CARD, RED_CARD, SUBSTITUTION
}

@Entity
@Table(name = "occurrence")
class Occurrence(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "match_sheet_id", nullable = false)
    val matchSheetId: Long,

    @Column(nullable = false)
    val minute: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: OccurrenceType,

    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "player_id", nullable = false)
    val playerId: Long,

    @Column(name = "replaced_player_id")
    val replacedPlayerId: Long? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
)
