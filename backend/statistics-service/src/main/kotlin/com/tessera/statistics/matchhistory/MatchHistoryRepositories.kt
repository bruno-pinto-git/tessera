package com.tessera.statistics.matchhistory

import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MatchSummaryRepository : JpaRepository<MatchSummary, Long>, JpaSpecificationExecutor<MatchSummary>

interface LineupSnapshotRepository : JpaRepository<LineupSnapshot, LineupSnapshotId> {
    @Query("SELECT l FROM LineupSnapshot l WHERE l.id.matchId = :matchId ORDER BY l.teamId, l.shirtNumber NULLS LAST")
    fun findByMatchId(@Param("matchId") matchId: Long): List<LineupSnapshot>

    fun deleteByIdMatchId(matchId: Long)

    @Query("""
        SELECT COUNT(DISTINCT l.id.matchId) FROM LineupSnapshot l
         WHERE l.id.playerId = :playerId
    """)
    fun countMatchesByPlayer(@Param("playerId") playerId: Long): Long
}

interface OccurrenceSnapshotRepository : JpaRepository<OccurrenceSnapshot, Long> {
    @Query("SELECT o FROM OccurrenceSnapshot o WHERE o.matchId = :matchId ORDER BY o.minute, o.occurrenceId")
    fun findByMatchId(@Param("matchId") matchId: Long): List<OccurrenceSnapshot>

    fun deleteByMatchId(matchId: Long)
}

/**
 * Composable predicates for MatchSummary filtering, mirroring the pattern
 * used by match-service for live Match queries.
 */
object MatchSummarySpecs {

    fun season(season: String?): Specification<MatchSummary>? =
        season?.let { Specification { root, _, cb -> cb.equal(root.get<String>("season"), it) } }

    fun status(status: String?): Specification<MatchSummary>? =
        status?.let { Specification { root, _, cb -> cb.equal(root.get<String>("matchStatus"), it) } }

    /**
     * Match is "by club" if either home_club_id or away_club_id matches.
     */
    fun byClub(clubId: Long?): Specification<MatchSummary>? =
        clubId?.let { id ->
            Specification { root, _, cb ->
                cb.or(
                    cb.equal(root.get<Long>("homeClubId"), id),
                    cb.equal(root.get<Long>("awayClubId"), id),
                )
            }
        }

    /**
     * Match has the given player in the lineup snapshot. Uses a JPQL subquery
     * via `criteriaBuilder.exists`.
     */
    fun byPlayer(playerId: Long?): Specification<MatchSummary>? =
        playerId?.let { id ->
            Specification { root, query, cb ->
                val sub = query!!.subquery(java.lang.Long::class.java)
                val l = sub.from(LineupSnapshot::class.java)
                sub.select(l.get<LineupSnapshotId>("id").get("matchId"))
                    .where(
                        cb.equal(l.get<LineupSnapshotId>("id").get<Long>("playerId"), id),
                        cb.equal(l.get<LineupSnapshotId>("id").get<Long>("matchId"), root.get<Long>("matchId")),
                    )
                cb.exists(sub)
            }
        }
}

fun MatchSummaryRepository.findFiltered(
    clubId: Long?,
    playerId: Long?,
    season: String?,
    status: String?,
    pageable: Pageable,
): Page<MatchSummary> {
    var spec: Specification<MatchSummary> = Specification { _, _, _ -> null as Predicate? }
    MatchSummarySpecs.byClub(clubId)?.let { spec = spec.and(it) }
    MatchSummarySpecs.byPlayer(playerId)?.let { spec = spec.and(it) }
    MatchSummarySpecs.season(season)?.let { spec = spec.and(it) }
    MatchSummarySpecs.status(status)?.let { spec = spec.and(it) }
    return findAll(spec, pageable)
}
