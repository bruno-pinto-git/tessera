package com.tessera.match.match

import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface MatchRepository : JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    @Query("SELECT m FROM Match m WHERE m.id = :id AND m.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): Match?
}

/**
 * Builds dynamic predicates for the matches list endpoint, filtering by any
 * combination of from / to / status / clubId. Always excludes soft-deleted.
 *
 * `clubId` requires joining team to find clubs of both home and away teams;
 * we keep it scoped to the SQL produced by the repository (no extra JPQL).
 */
object MatchSpecs {

    fun active(): Specification<Match> = Specification { root, _, cb ->
        cb.isNull(root.get<OffsetDateTime?>("deletedAt"))
    }

    fun kickoffAtFrom(from: OffsetDateTime?): Specification<Match>? =
        from?.let { Specification { root, _, cb ->
            cb.greaterThanOrEqualTo(root.get("kickoffAt"), it)
        } }

    fun kickoffAtBefore(to: OffsetDateTime?): Specification<Match>? =
        to?.let { Specification { root, _, cb ->
            cb.lessThan(root.get("kickoffAt"), it)
        } }

    fun status(status: MatchStatus?): Specification<Match>? =
        status?.let { Specification { root, _, cb ->
            cb.equal(root.get<MatchStatus>("status"), it)
        } }

    /**
     * Subquery: match.home_team_id OR match.away_team_id is in the set of team
     * ids whose club_id = :clubId AND team is active. Subquery is the cleanest
     * way without modeling Match→Team as an association.
     */
    fun involvesClub(clubId: Long?): Specification<Match>? =
        clubId?.let { Specification { root, query, cb ->
            val homeSub = query!!.subquery(java.lang.Long::class.java)
            val homeT = homeSub.from(com.tessera.match.team.Team::class.java)
            homeSub.select(homeT.get("id"))
                .where(
                    cb.equal(homeT.get<Long>("clubId"), it),
                    cb.isNull(homeT.get<OffsetDateTime?>("deletedAt")),
                )

            val awaySub = query.subquery(java.lang.Long::class.java)
            val awayT = awaySub.from(com.tessera.match.team.Team::class.java)
            awaySub.select(awayT.get("id"))
                .where(
                    cb.equal(awayT.get<Long>("clubId"), it),
                    cb.isNull(awayT.get<OffsetDateTime?>("deletedAt")),
                )

            cb.or(
                root.get<Long>("homeTeamId").`in`(homeSub),
                root.get<Long>("awayTeamId").`in`(awaySub),
            )
        } }
}

fun MatchRepository.findFiltered(
    from: OffsetDateTime?,
    to: OffsetDateTime?,
    status: MatchStatus?,
    clubId: Long?,
    pageable: Pageable,
): Page<Match> {
    var spec: Specification<Match> = MatchSpecs.active()
    MatchSpecs.kickoffAtFrom(from)?.let { spec = spec.and(it) }
    MatchSpecs.kickoffAtBefore(to)?.let { spec = spec.and(it) }
    MatchSpecs.status(status)?.let { spec = spec.and(it) }
    MatchSpecs.involvesClub(clubId)?.let { spec = spec.and(it) }
    return findAll(spec, pageable)
}
