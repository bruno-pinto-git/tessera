package com.tessera.match.club

import com.tessera.match.iam.KeycloakGroupService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [ClubService] — name-uniqueness rules and the
 * save-then-provision-Keycloak flow (with its rollback-on-failure path).
 * Repositories and the Keycloak group service are mocked.
 */
class ClubServiceTest {

    private val repo: ClubRepository = mock()
    private val keycloakGroups: KeycloakGroupService = mock()
    private val service = ClubService(repo, keycloakGroups)

    // ----- create -------------------------------------------------------------

    @Test
    fun `create rejects a duplicate name`() {
        whenever(repo.existsActiveByNameIgnoreCase("Sporting")).thenReturn(true)
        assertFailsWith<ClubNameConflictException> { service.create(ClubCreateRequest(name = "Sporting")) }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create persists the club and provisions Keycloak groups`() {
        whenever(repo.existsActiveByNameIgnoreCase("Sporting")).thenReturn(false)
        doReturn(club(id = 1L, name = "Sporting")).whenever(repo).save(any())

        val created = service.create(ClubCreateRequest(name = "  Sporting  "))

        assertEquals("Sporting", created.name)
        verify(keycloakGroups).ensureClubGroups(1L)
    }

    @Test
    fun `create rolls back as a provisioning error when Keycloak fails`() {
        whenever(repo.existsActiveByNameIgnoreCase("Sporting")).thenReturn(false)
        doReturn(club(id = 1L, name = "Sporting")).whenever(repo).save(any())
        whenever(keycloakGroups.ensureClubGroups(1L)).thenThrow(RuntimeException("keycloak down"))

        assertFailsWith<ClubProvisioningException> { service.create(ClubCreateRequest(name = "Sporting")) }
    }

    // ----- update -------------------------------------------------------------

    @Test
    fun `update fails for an unknown club`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<ClubNotFoundException> { service.update(404L, ClubUpdateRequest(name = "X")) }
    }

    @Test
    fun `update rejects renaming to an existing club name`() {
        whenever(repo.findActiveById(1L)).thenReturn(club(1L, "Sporting"))
        whenever(repo.existsActiveByNameIgnoreCaseExcluding("Benfica", 1L)).thenReturn(true)
        assertFailsWith<ClubNameConflictException> {
            service.update(1L, ClubUpdateRequest(name = "Benfica"))
        }
    }

    @Test
    fun `update keeping the same name (case-insensitive) skips the conflict check`() {
        whenever(repo.findActiveById(1L)).thenReturn(club(1L, "Sporting"))

        val updated = service.update(1L, ClubUpdateRequest(name = "sporting", foundedYear = 1906))

        assertEquals("sporting", updated.name)
        assertEquals(1906, updated.foundedYear)
        verify(repo, never()).existsActiveByNameIgnoreCaseExcluding(any(), any())
    }

    // ----- delete / get / list ------------------------------------------------

    @Test
    fun `delete soft-deletes the club`() {
        val club = club(1L, "Sporting")
        whenever(repo.findActiveById(1L)).thenReturn(club)
        service.delete(1L)
        assert(club.deletedAt != null)
    }

    @Test
    fun `get fails for an unknown club`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<ClubNotFoundException> { service.get(404L) }
    }

    @Test
    fun `list without a name filter returns all active clubs`() {
        val page: Page<Club> = PageImpl(listOf(club(1L, "Sporting")))
        whenever(repo.findAllActive(Pageable.unpaged())).thenReturn(page)
        assertEquals(1, service.list(null, Pageable.unpaged()).totalElements.toInt())
    }

    @Test
    fun `list with a name filter searches by name`() {
        val page: Page<Club> = PageImpl(listOf(club(1L, "Sporting")))
        whenever(repo.findActiveByNameLike("Spor", Pageable.unpaged())).thenReturn(page)
        assertEquals(1, service.list("Spor", Pageable.unpaged()).totalElements.toInt())
    }

    // -------------------------------------------------------------------------

    private fun club(id: Long, name: String) = Club(
        id = id,
        name = name,
        createdAt = OffsetDateTime.now(),
    )
}
