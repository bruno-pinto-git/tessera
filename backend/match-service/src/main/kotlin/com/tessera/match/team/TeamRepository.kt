package com.tessera.match.team

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TeamRepository : JpaRepository<Team, Long> {

    @Query("""
        SELECT t FROM Team t
         WHERE t.clubId = :clubId
           AND t.deletedAt IS NULL
         ORDER BY t.category
    """)
    fun findActiveByClub(@Param("clubId") clubId: Long): List<Team>

    @Query("SELECT t FROM Team t WHERE t.id = :id AND t.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): Team?

    @Query("""
        SELECT COUNT(t) > 0 FROM Team t
         WHERE t.clubId = :clubId
           AND t.category = :category
           AND t.deletedAt IS NULL
    """)
    fun existsActiveByClubAndCategory(
        @Param("clubId") clubId: Long,
        @Param("category") category: TeamCategory,
    ): Boolean

    @Query("""
        SELECT COUNT(t) > 0 FROM Team t
         WHERE t.clubId = :clubId
           AND t.category = :category
           AND t.deletedAt IS NULL
           AND t.id <> :excludeId
    """)
    fun existsActiveByClubAndCategoryExcluding(
        @Param("clubId") clubId: Long,
        @Param("category") category: TeamCategory,
        @Param("excludeId") excludeId: Long,
    ): Boolean
}
