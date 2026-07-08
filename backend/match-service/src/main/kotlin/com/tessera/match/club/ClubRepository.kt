package com.tessera.match.club

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubRepository : JpaRepository<Club, Long> {

    @Query("SELECT c FROM Club c WHERE c.deletedAt IS NULL")
    fun findAllActive(pageable: Pageable): Page<Club>

    @Query("""
        SELECT c FROM Club c
         WHERE c.deletedAt IS NULL
           AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    fun findActiveByNameLike(@Param("name") name: String, pageable: Pageable): Page<Club>

    @Query("SELECT c FROM Club c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): Club?

    @Query("""
        SELECT COUNT(c) > 0 FROM Club c
         WHERE LOWER(c.name) = LOWER(:name)
           AND c.deletedAt IS NULL
    """)
    fun existsActiveByNameIgnoreCase(@Param("name") name: String): Boolean

    @Query("""
        SELECT COUNT(c) > 0 FROM Club c
         WHERE LOWER(c.name) = LOWER(:name)
           AND c.deletedAt IS NULL
           AND c.id <> :excludeId
    """)
    fun existsActiveByNameIgnoreCaseExcluding(
        @Param("name") name: String,
        @Param("excludeId") excludeId: Long,
    ): Boolean
}
