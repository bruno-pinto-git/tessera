package com.tessera.match.player

import com.tessera.match.club.ClubNotFoundException
import com.tessera.match.club.ClubRepository
import com.tessera.match.team.Team
import com.tessera.match.team.TeamCategory
import com.tessera.match.team.TeamNotFoundException
import com.tessera.match.team.TeamRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Unit tests for [PlayerService] — shirt-number uniqueness and existence
 * checks, mirroring docs/http-tests/04-players.http. Repositories are mocked.
 */
class PlayerServiceTest {

    private val repo: PlayerRepository = mock()
    private val teamRepo: TeamRepository = mock()
    private val clubRepo: ClubRepository = mock()

    private val service = PlayerService(repo, teamRepo, clubRepo)

    // ----- create -------------------------------------------------------------

    @Test
    fun `create fails when the team does not exist`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(null)
        assertFailsWith<TeamNotFoundException> { service.create(1L, createReq(shirt = 7)) }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create rejects a duplicate shirt number on the same team`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        whenever(repo.existsActiveByTeamAndShirt(1L, 7)).thenReturn(true)
        assertFailsWith<PlayerShirtConflictException> { service.create(1L, createReq(shirt = 7)) }
    }

    @Test
    fun `create succeeds with a free shirt number`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        whenever(repo.existsActiveByTeamAndShirt(1L, 7)).thenReturn(false)
        doReturn(player(1L, teamId = 1L, shirt = 7)).whenever(repo).save(any())

        val created = service.create(1L, createReq(shirt = 7))

        assertEquals(1L, created.teamId)
        assertEquals(7, created.shirtNumber)
        verify(repo).save(any())
    }

    @Test
    fun `create skips the shirt check when no number is given`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        doReturn(player(1L, teamId = 1L, shirt = null)).whenever(repo).save(any())

        service.create(1L, createReq(shirt = null))

        verify(repo, never()).existsActiveByTeamAndShirt(any(), any())
    }

    // ----- update -------------------------------------------------------------

    @Test
    fun `update fails for an unknown player`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<PlayerNotFoundException> {
            service.update(404L, PlayerUpdateRequest(shirtNumber = 9))
        }
    }

    @Test
    fun `update rejects a shirt number already used by another player`() {
        whenever(repo.findActiveById(11L)).thenReturn(player(11L, teamId = 1L, shirt = 7))
        whenever(repo.existsActiveByTeamAndShirtExcluding(1L, 9, 11L)).thenReturn(true)
        assertFailsWith<PlayerShirtConflictException> {
            service.update(11L, PlayerUpdateRequest(shirtNumber = 9))
        }
    }

    @Test
    fun `update keeping the same shirt number does not trigger a conflict check`() {
        whenever(repo.findActiveById(11L)).thenReturn(player(11L, teamId = 1L, shirt = 7))

        val updated = service.update(11L, PlayerUpdateRequest(shirtNumber = 7))

        assertEquals(7, updated.shirtNumber)
        verify(repo, never()).existsActiveByTeamAndShirtExcluding(any(), any(), any())
    }

    // ----- delete / get / listByClub -----------------------------------------

    @Test
    fun `delete soft-deletes the player`() {
        val player = player(11L, teamId = 1L, shirt = 7)
        whenever(repo.findActiveById(11L)).thenReturn(player)

        service.delete(11L)

        assert(player.deletedAt != null) { "expected deletedAt to be stamped" }
    }

    @Test
    fun `get fails for an unknown player`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<PlayerNotFoundException> { service.get(404L) }
    }

    @Test
    fun `listByClub fails when the club does not exist`() {
        whenever(clubRepo.findActiveById(99L)).thenReturn(null)
        assertFailsWith<ClubNotFoundException> { service.listByClub(99L, Pageable.unpaged()) }
    }

    // -------------------------------------------------------------------------
    // Fixtures
    // -------------------------------------------------------------------------

    private fun createReq(shirt: Int?) = PlayerCreateRequest(
        firstName = "Test",
        lastName = "Player",
        position = PlayerPosition.MF,
        shirtNumber = shirt,
    )

    private fun team(id: Long) = Team(
        id = id,
        clubId = id * 10,
        category = TeamCategory.SENIOR_M,
        createdAt = OffsetDateTime.now(),
    )

    private fun player(id: Long, teamId: Long, shirt: Int?) = Player(
        id = id,
        teamId = teamId,
        firstName = "Test",
        lastName = "Player",
        position = PlayerPosition.MF,
        shirtNumber = shirt,
        createdAt = OffsetDateTime.now(),
    )
}
