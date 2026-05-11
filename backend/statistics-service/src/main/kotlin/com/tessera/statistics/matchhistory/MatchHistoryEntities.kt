package com.tessera.statistics.matchhistory

import jakarta.persistence.*
import java.io.Serializable
import java.time.OffsetDateTime

@Entity
@Table(name = "match_summary")
class MatchSummary(
    @Id
    @Column(name = "match_id")
    val matchId: Long,

    @Column(nullable = false, length = 10)
    val season: String,

    @Column(name = "match_status", nullable = false, length = 20)
    val matchStatus: String,

    @Column(name = "kickoff_at", nullable = false)
    val kickoffAt: OffsetDateTime,

    @Column(name = "home_team_id", nullable = false)
    val homeTeamId: Long,

    @Column(name = "away_team_id", nullable = false)
    val awayTeamId: Long,

    @Column(name = "home_club_id", nullable = false)
    val homeClubId: Long,

    @Column(name = "away_club_id", nullable = false)
    val awayClubId: Long,

    @Column(name = "venue_id")
    val venueId: Long?,

    @Column(name = "home_score")
    val homeScore: Int?,

    @Column(name = "away_score")
    val awayScore: Int?,

    @Column(name = "referee_name", length = 200)
    val refereeName: String?,

    @Column(name = "snapshot_at", nullable = false)
    val snapshotAt: OffsetDateTime = OffsetDateTime.now(),
)

@Embeddable
data class LineupSnapshotId(
    @Column(name = "match_id")
    val matchId: Long = 0,
    @Column(name = "player_id")
    val playerId: Long = 0,
) : Serializable

@Entity
@Table(name = "lineup_snapshot")
class LineupSnapshot(
    @EmbeddedId
    val id: LineupSnapshotId,

    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "shirt_number")
    val shirtNumber: Int?,

    @Column(nullable = false, length = 10)
    val role: String,
)

@Entity
@Table(name = "occurrence_snapshot")
class OccurrenceSnapshot(
    @Id
    @Column(name = "occurrence_id")
    val occurrenceId: Long,

    @Column(name = "match_id", nullable = false)
    val matchId: Long,

    @Column(nullable = false)
    val minute: Int,

    @Column(nullable = false, length = 20)
    val type: String,

    @Column(name = "team_id", nullable = false)
    val teamId: Long,

    @Column(name = "player_id", nullable = false)
    val playerId: Long,

    @Column(name = "replaced_player_id")
    val replacedPlayerId: Long?,
)
