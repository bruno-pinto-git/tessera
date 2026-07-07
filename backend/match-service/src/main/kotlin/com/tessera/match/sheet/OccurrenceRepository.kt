package com.tessera.match.sheet

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OccurrenceRepository : JpaRepository<Occurrence, Long> {

    @Query("""
        SELECT o FROM Occurrence o
         WHERE o.matchSheetId = :sheetId
         ORDER BY o.minute ASC, o.id ASC
    """)
    fun findBySheet(@Param("sheetId") sheetId: Long): List<Occurrence>

    @Query("""
        SELECT COUNT(o) FROM Occurrence o
         WHERE o.matchSheetId = :sheetId
           AND o.teamId = :teamId
           AND o.type = :type
    """)
    fun countBySheetTeamType(
        @Param("sheetId") sheetId: Long,
        @Param("teamId") teamId: Long,
        @Param("type") type: OccurrenceType,
    ): Long

    @Query("""
        SELECT COUNT(o) > 0 FROM Occurrence o
         WHERE o.matchSheetId = :sheetId
           AND o.playerId = :playerId
           AND o.type = com.tessera.match.sheet.OccurrenceType.RED_CARD
    """)
    fun playerHasRedCard(
        @Param("sheetId") sheetId: Long,
        @Param("playerId") playerId: Long,
    ): Boolean
}
