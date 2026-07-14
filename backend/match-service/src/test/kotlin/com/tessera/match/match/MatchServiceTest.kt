package com.tessera.match.match

import com.tessera.match.sheet.MatchSheetService
import com.tessera.match.team.Team
import com.tessera.match.team.TeamCategory
import com.tessera.match.team.TeamNotFoundException
import com.tessera.match.team.TeamRepository
import com.tessera.match.venue.VenueNotFoundException
import com.tessera.match.venue.VenueRepository
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

class MatchServiceTest {

    private val repo: MatchRepository = mock()
    private val teamRepo: TeamRepository = mock()
    private val venueRepo: VenueRepository = mock()
    private val sheetService: MatchSheetService = mock()

    private val service = MatchService(repo, teamRepo, venueRepo, sheetService)

    @Test
    fun `create rejects identical home and away team`() {
        val req = createReq(home = 1L, away = 1L)
        assertFailsWith<IllegalArgumentException> { service.create(req) }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create rejects a kickoff in the past`() {
        val req = createReq(home = 1L, away = 2L, kickoffAt = OffsetDateTime.now().minusDays(1))
        assertFailsWith<IllegalArgumentException> { service.create(req) }
        verify(repo, never()).save(any())
    }

    @Test
    fun `create fails when home team does not exist`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(null)
        assertFailsWith<TeamNotFoundException> { service.create(createReq(home = 1L, away = 2L)) }
    }

    @Test
    fun `create fails when away team does not exist`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        whenever(teamRepo.findActiveById(2L)).thenReturn(null)
        assertFailsWith<TeamNotFoundException> { service.create(createReq(home = 1L, away = 2L)) }
    }

    @Test
    fun `create fails when venue does not exist`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        whenever(teamRepo.findActiveById(2L)).thenReturn(team(2L))
        whenever(venueRepo.findActiveById(99L)).thenReturn(null)
        val req = createReq(home = 1L, away = 2L, venueId = 99L)
        assertFailsWith<VenueNotFoundException> { service.create(req) }
    }

    @Test
    fun `create persists a scheduled match on the happy path`() {
        whenever(teamRepo.findActiveById(1L)).thenReturn(team(1L))
        whenever(teamRepo.findActiveById(2L)).thenReturn(team(2L))
        doReturn(match(id = 1L, status = MatchStatus.SCHEDULED)).whenever(repo).save(any())

        val created = service.create(createReq(home = 1L, away = 2L))

        assertEquals(1L, created.homeTeamId)
        assertEquals(2L, created.awayTeamId)
        assertEquals(MatchStatus.SCHEDULED, created.status)
        verify(repo).save(any())
    }

    @Test
    fun `valid transition scheduled to live is applied`() {
        val match = match(id = 5L, status = MatchStatus.SCHEDULED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        val updated = service.update(5L, MatchUpdateRequest(status = MatchStatus.LIVE))

        assertEquals(MatchStatus.LIVE, updated.status)
    }

    @Test
    fun `invalid transition scheduled to finished is rejected`() {
        val match = match(id = 5L, status = MatchStatus.SCHEDULED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        assertFailsWith<InvalidMatchTransitionException> {
            service.update(5L, MatchUpdateRequest(status = MatchStatus.FINISHED))
        }
    }

    @Test
    fun `terminal status is immutable`() {
        val match = match(id = 5L, status = MatchStatus.CANCELLED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        assertFailsWith<InvalidMatchTransitionException> {
            service.update(5L, MatchUpdateRequest(status = MatchStatus.SCHEDULED))
        }
    }

    @Test
    fun `same status is a no-op and does not require a legal transition`() {
        val match = match(id = 5L, status = MatchStatus.FINISHED, homeScore = 1, awayScore = 0)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        val updated = service.update(5L, MatchUpdateRequest(status = MatchStatus.FINISHED))
        assertEquals(MatchStatus.FINISHED, updated.status)
    }

    @Test
    fun `finishing a match requires both scores`() {
        val match = match(id = 5L, status = MatchStatus.LIVE)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        assertFailsWith<IllegalArgumentException> {
            service.update(5L, MatchUpdateRequest(status = MatchStatus.FINISHED, homeScore = 2))
        }
    }

    @Test
    fun `finishing a match with both scores succeeds`() {
        val match = match(id = 5L, status = MatchStatus.LIVE)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        val updated = service.update(
            5L, MatchUpdateRequest(status = MatchStatus.FINISHED, homeScore = 2, awayScore = 1),
        )

        assertEquals(MatchStatus.FINISHED, updated.status)
        assertEquals(2, updated.homeScore)
        assertEquals(1, updated.awayScore)
    }

    @Test
    fun `entering a terminal status auto-locks the sheet`() {
        val match = match(id = 5L, status = MatchStatus.SCHEDULED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        service.update(5L, MatchUpdateRequest(status = MatchStatus.CANCELLED))

        verify(sheetService).autoLockIfPresent(5L)
    }

    @Test
    fun `non-terminal transition does not touch the sheet`() {
        val match = match(id = 5L, status = MatchStatus.SCHEDULED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        service.update(5L, MatchUpdateRequest(status = MatchStatus.LIVE))

        verify(sheetService, never()).autoLockIfPresent(any())
    }

    @Test
    fun `update fails for an unknown match`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<MatchNotFoundException> {
            service.update(404L, MatchUpdateRequest(status = MatchStatus.LIVE))
        }
    }

    @Test
    fun `delete soft-deletes the match`() {
        val match = match(id = 5L, status = MatchStatus.SCHEDULED)
        whenever(repo.findActiveById(5L)).thenReturn(match)

        service.delete(5L)

        assert(match.deletedAt != null) { "expected deletedAt to be stamped" }
    }

    @Test
    fun `get fails for an unknown match`() {
        whenever(repo.findActiveById(404L)).thenReturn(null)
        assertFailsWith<MatchNotFoundException> { service.get(404L) }
    }

    private fun createReq(
        home: Long,
        away: Long,
        venueId: Long? = null,
        kickoffAt: OffsetDateTime = OffsetDateTime.now().plusDays(1),
    ) = MatchCreateRequest(
        homeTeamId = home,
        awayTeamId = away,
        venueId = venueId,
        kickoffAt = kickoffAt,
    )

    private fun team(id: Long) = Team(
        id = id,
        clubId = id * 10,
        category = TeamCategory.SENIOR_M,
        createdAt = OffsetDateTime.now(),
    )

    private fun match(
        id: Long,
        status: MatchStatus,
        homeScore: Int? = null,
        awayScore: Int? = null,
    ) = Match(
        id = id,
        homeTeamId = 1L,
        awayTeamId = 2L,
        kickoffAt = OffsetDateTime.now().plusDays(1),
        status = status,
        homeScore = homeScore,
        awayScore = awayScore,
    )
}
