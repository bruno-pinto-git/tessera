package com.tessera.match.player

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PlayerRepository : JpaRepository<Player, Long> {

    @Query("""
        SELECT p FROM Player p
         WHERE p.teamId = :teamId
           AND p.deletedAt IS NULL
         ORDER BY
           CASE WHEN p.shirtNumber IS NULL THEN 1 ELSE 0 END,
           p.shirtNumber ASC,
           p.lastName ASC
    """)
    fun findActiveByTeam(@Param("teamId") teamId: Long, pageable: Pageable): Page<Player>

    /**
     * Returns active players belonging to any active team of the given club.
     * Joins Player → Team (via team_id) and filters by club_id.
     */
    @Query("""
        SELECT p FROM Player p, com.tessera.match.team.Team t
         WHERE p.teamId = t.id
           AND t.clubId = :clubId
           AND p.deletedAt IS NULL
           AND t.deletedAt IS NULL
         ORDER BY p.lastName ASC, p.firstName ASC
    """)
    fun findActiveByClub(@Param("clubId") clubId: Long, pageable: Pageable): Page<Player>

    @Query("SELECT p FROM Player p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): Player?

    @Query("""
        SELECT COUNT(p) > 0 FROM Player p
         WHERE p.teamId = :teamId
           AND p.shirtNumber = :shirtNumber
           AND p.deletedAt IS NULL
    """)
    fun existsActiveByTeamAndShirt(
        @Param("teamId") teamId: Long,
        @Param("shirtNumber") shirtNumber: Int,
    ): Boolean

    @Query("""
        SELECT COUNT(p) > 0 FROM Player p
         WHERE p.teamId = :teamId
           AND p.shirtNumber = :shirtNumber
           AND p.deletedAt IS NULL
           AND p.id <> :excludeId
    """)
    fun existsActiveByTeamAndShirtExcluding(
        @Param("teamId") teamId: Long,
        @Param("shirtNumber") shirtNumber: Int,
        @Param("excludeId") excludeId: Long,
    ): Boolean
}
