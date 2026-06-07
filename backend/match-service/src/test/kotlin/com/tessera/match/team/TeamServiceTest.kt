package com.tessera.match.team

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import com.tessera.match.club.Club
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [TeamService] — one team per (club, category) and club
 * existence checks. Repositories are mocked.
 */
class TeamServiceTest {

    private val repo: TeamRepository = mock()
    private val clubRepo: ClubRepository = mock()
    private val service = TeamService(repo, clubRepo)

    @Test
    fun `create fails when the club does not exist`() {
        whenever(clubRepo.findActiveById(1L)).thenReturn(null)
        assertFailsWith<ClubNotFoundException> {
            service.create(1L, TeamCreateRequest(TeamCategory.SENIOR_M))
        }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create rejects a duplicate category for the club`() {
        whenever(clubRepo.findActiveById(1L)).thenReturn(club(1L))
        whenever(repo.existsActiveByClubAndCategory(1L, TeamCategory.SENIOR_M)).thenReturn(true)
        assertFailsWith<TeamCategoryConflictException> {
            service.create(1L, TeamCreateRequest(TeamCategory.SENIOR_M))
        }
    }

    @Test
    fun `create succeeds for a new category`() {
        whenever(clubRepo.findActiveById(1L)).thenReturn(club(1L))
        whenever(repo.existsActiveByClubAndCategory(1L, TeamCategory.SENIOR_M)).thenReturn(false)
        doReturn(team(7L, 1L, TeamCategory.SENIOR_M)).whenever(repo).save(any())

        val created = service.create(1L, TeamCreateRequest(TeamCategory.SENIOR_M))

        assertEquals(1L, created.clubId)
        assertEquals(TeamCategory.SENIOR_M, created.category)
    }

    @Test
    fun `update fails for an unknown team`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<TeamNotFoundException> {
            service.update(404L, TeamUpdateRequest(TeamCategory.SUB_19))
        }
    }

    @Test
    fun `update rejects changing to a category already used by the club`() {
        whenever(repo.findActiveById(7L)).thenReturn(team(7L, 1L, TeamCategory.SENIOR_M))
        whenever(repo.existsActiveByClubAndCategoryExcluding(1L, TeamCategory.SUB_19, 7L)).thenReturn(true)
        assertFailsWith<TeamCategoryConflictException> {
            service.update(7L, TeamUpdateRequest(TeamCategory.SUB_19))
        }
    }

    @Test
    fun `update to a free category succeeds`() {
        whenever(repo.findActiveById(7L)).thenReturn(team(7L, 1L, TeamCategory.SENIOR_M))
        whenever(repo.existsActiveByClubAndCategoryExcluding(1L, TeamCategory.SUB_19, 7L)).thenReturn(false)

        val updated = service.update(7L, TeamUpdateRequest(TeamCategory.SUB_19))

        assertEquals(TeamCategory.SUB_19, updated.category)
    }

    @Test
    fun `update keeping the same category is a no-op`() {
        whenever(repo.findActiveById(7L)).thenReturn(team(7L, 1L, TeamCategory.SENIOR_M))

        service.update(7L, TeamUpdateRequest(TeamCategory.SENIOR_M))

        verify(repo, never()).existsActiveByClubAndCategoryExcluding(any(), any(), any())
    }

    @Test
    fun `delete soft-deletes the team`() {
        val team = team(7L, 1L, TeamCategory.SENIOR_M)
        whenever(repo.findActiveById(7L)).thenReturn(team)
        service.delete(7L)
        assert(team.deletedAt != null)
    }

    @Test
    fun `get fails for an unknown team`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<TeamNotFoundException> { service.get(404L) }
    }

    @Test
    fun `listByClub fails when the club does not exist`() {
        whenever(clubRepo.findActiveById(99L)).thenReturn(null)
        assertFailsWith<ClubNotFoundException> { service.listByClub(99L) }
    }

    // -------------------------------------------------------------------------

    private fun club(id: Long) = Club(id = id, name = "Club $id", createdAt = OffsetDateTime.now())

    private fun team(id: Long, clubId: Long, category: TeamCategory) =
        Team(id = id, clubId = clubId, category = category, createdAt = OffsetDateTime.now())
}
