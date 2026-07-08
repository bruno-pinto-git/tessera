package com.tessera.match.match

import jakarta.persistence.*
import java.time.OffsetDateTime

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    POSTPONED,
    ABANDONED,
    CANCELLED,
}

@Entity
@Table(name = "match")
class Match(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "home_team_id", nullable = false)
    val homeTeamId: Long,

    @Column(name = "away_team_id", nullable = false)
    val awayTeamId: Long,

    @Column(name = "venue_id")
    var venueId: Long? = null,

    @Column(name = "kickoff_at", nullable = false)
    var kickoffAt: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchStatus = MatchStatus.SCHEDULED,

    @Column(name = "home_score")
    var homeScore: Int? = null,

    @Column(name = "away_score")
    var awayScore: Int? = null,

    @Column(name = "referee_name", length = 200)
    var refereeName: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null,
)
