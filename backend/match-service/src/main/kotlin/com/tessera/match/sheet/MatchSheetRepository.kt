package com.tessera.match.sheet

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MatchSheetRepository : JpaRepository<MatchSheet, Long> {

    @Query("SELECT s FROM MatchSheet s WHERE s.matchId = :matchId")
    fun findByMatchId(@Param("matchId") matchId: Long): MatchSheet?
}

interface LineupEntryRepository : JpaRepository<LineupEntry, LineupEntryId> {

    @Query("""
        SELECT e FROM LineupEntry e
         WHERE e.id.matchSheetId = :sheetId
         ORDER BY e.teamId,
           CASE WHEN e.shirtNumber IS NULL THEN 1 ELSE 0 END,
           e.shirtNumber
    """)
    fun findBySheet(@Param("sheetId") sheetId: Long): List<LineupEntry>

    @Query("""
        SELECT COUNT(e) > 0 FROM LineupEntry e
         WHERE e.id.matchSheetId = :sheetId
           AND e.teamId = :teamId
           AND e.shirtNumber = :shirtNumber
           AND e.id.playerId <> :excludePlayerId
    """)
    fun existsBySheetTeamShirtExcluding(
        @Param("sheetId") sheetId: Long,
        @Param("teamId") teamId: Long,
        @Param("shirtNumber") shirtNumber: Int,
        @Param("excludePlayerId") excludePlayerId: Long,
    ): Boolean

    @Query("""
        SELECT COUNT(e) > 0 FROM LineupEntry e
         WHERE e.id.matchSheetId = :sheetId
           AND e.teamId = :teamId
           AND e.shirtNumber = :shirtNumber
    """)
    fun existsBySheetTeamShirt(
        @Param("sheetId") sheetId: Long,
        @Param("teamId") teamId: Long,
        @Param("shirtNumber") shirtNumber: Int,
    ): Boolean
}
