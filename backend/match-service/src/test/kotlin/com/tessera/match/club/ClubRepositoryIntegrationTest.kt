package com.tessera.match.club

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration test for [ClubRepository] against a real Postgres (Testcontainers),
 * with the schema built by the actual Flyway migrations (V1..V10). This verifies
 * the custom @Query JPQL that the service-layer unit tests only mocked:
 * case-insensitive name matching and the soft-delete (deletedAt IS NULL) filters.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ClubRepositoryIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired private lateinit var repo: ClubRepository

    @Test
    fun `name uniqueness is case-insensitive and ignores soft-deleted rows`() {
        repo.save(Club(name = "Sporting CP"))

        assertTrue(repo.existsActiveByNameIgnoreCase("sporting cp"))
        assertTrue(repo.existsActiveByNameIgnoreCase("SPORTING CP"))
        assertFalse(repo.existsActiveByNameIgnoreCase("Benfica"))
    }

    @Test
    fun `findActiveById and findAllActive exclude soft-deleted clubs`() {
        val club = repo.save(Club(name = "Porto"))
        val id = club.id

        // Soft delete.
        club.deletedAt = OffsetDateTime.now()
        repo.save(club)

        assertNull(repo.findActiveById(id))
        assertFalse(repo.findAllActive(PageRequest.of(0, 50)).content.any { it.id == id })
        // A soft-deleted name should no longer count as a conflict.
        assertFalse(repo.existsActiveByNameIgnoreCase("Porto"))
    }

    @Test
    fun `findActiveByNameLike matches a partial, case-insensitive substring`() {
        repo.save(Club(name = "Vitoria SC"))
        repo.save(Club(name = "Vitoria FC"))
        repo.save(Club(name = "Boavista"))

        val matches = repo.findActiveByNameLike("vitoria", PageRequest.of(0, 50)).content
        assertEquals(2, matches.size)
    }

    @Test
    fun `existsActiveByNameIgnoreCaseExcluding ignores the row being updated`() {
        val club = repo.save(Club(name = "Maritimo"))

        // Same row excluded -> no conflict; a different id -> conflict.
        assertFalse(repo.existsActiveByNameIgnoreCaseExcluding("maritimo", club.id))
        assertTrue(repo.existsActiveByNameIgnoreCaseExcluding("maritimo", club.id + 999))
    }
}
