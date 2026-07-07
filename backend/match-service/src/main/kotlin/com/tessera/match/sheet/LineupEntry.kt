package com.tessera.match.sheet

import jakarta.persistence.*
import java.io.Serializable

enum class LineupRole { STARTER, SUBSTITUTE }

@Embeddable
data class LineupEntryId(
    @Column(name = "match_sheet_id")
    val matchSheetId: Long = 0,

    @Column(name = "player_id")
    val playerId: Long = 0,
) : Serializable

@Entity
@Table(name = "lineup_entry")
class LineupEntry(
    @EmbeddedId
    val id: LineupEntryId,

    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "shirt_number")
    var shirtNumber: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var role: LineupRole,
)
