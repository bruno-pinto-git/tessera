package com.tessera.match.venue

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VenueRepository : JpaRepository<Venue, Long> {

    @Query("SELECT v FROM Venue v WHERE v.deletedAt IS NULL")
    fun findAllActive(pageable: Pageable): Page<Venue>

    @Query("""
        SELECT v FROM Venue v
         WHERE v.deletedAt IS NULL
           AND LOWER(v.name) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    fun findActiveByNameLike(@Param("name") name: String, pageable: Pageable): Page<Venue>

    @Query("SELECT v FROM Venue v WHERE v.id = :id AND v.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): Venue?

    @Query("""
        SELECT COUNT(v) > 0 FROM Venue v
         WHERE LOWER(v.name) = LOWER(:name)
           AND v.deletedAt IS NULL
    """)
    fun existsActiveByNameIgnoreCase(@Param("name") name: String): Boolean

    @Query("""
        SELECT COUNT(v) > 0 FROM Venue v
         WHERE LOWER(v.name) = LOWER(:name)
           AND v.deletedAt IS NULL
           AND v.id <> :excludeId
    """)
    fun existsActiveByNameIgnoreCaseExcluding(
        @Param("name") name: String,
        @Param("excludeId") excludeId: Long,
    ): Boolean
}
