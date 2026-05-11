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
}
