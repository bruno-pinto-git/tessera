package com.tessera.match.sheet

import com.tessera.match.events.MatchEventPublisher
import com.tessera.match.match.Match
import com.tessera.match.match.MatchStatus
import com.tessera.match.player.Player
import com.tessera.match.player.PlayerNotFoundException
import com.tessera.match.player.PlayerPosition
import com.tessera.match.player.PlayerRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MatchSheetServiceTest {

    private val sheetRepo: MatchSheetRepository = mock()
    private val lineupRepo: LineupEntryRepository = mock()
    private val occurrenceRepo: OccurrenceRepository = mock()
    private val matchRepo: com.tessera.match.match.MatchRepository = mock()
    private val playerRepo: PlayerRepository = mock()
    private val publisher: MatchEventPublisher = mock()

    private val service = MatchSheetService(
        sheetRepo, lineupRepo, occurrenceRepo, matchRepo, playerRepo, publisher,
    )

    private val MATCH_ID = 5L
    private val SHEET_ID = 100L
    private val HOME_TEAM = 1L
    private val AWAY_TEAM = 2L

    @Test
    fun `lineup add fails when player does not exist`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(null)
        assertFailsWith<PlayerNotFoundException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `lineup add fails when player belongs to neither team`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, teamId = 99L))
        assertFailsWith<IllegalArgumentException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `lineup add rejects a duplicate player`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, HOME_TEAM))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L)))
            .thenReturn(Optional.of(entry(7L, HOME_TEAM, LineupRole.STARTER)))
        assertFailsWith<LineupConflictException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `lineup add rejects a duplicate shirt number within the team`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, HOME_TEAM, shirt = 10))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L))).thenReturn(Optional.empty())
        whenever(lineupRepo.existsBySheetTeamShirt(SHEET_ID, HOME_TEAM, 10)).thenReturn(true)
        assertFailsWith<LineupConflictException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `lineup add rejects a 12th starter`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, HOME_TEAM))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L))).thenReturn(Optional.empty())
        whenever(lineupRepo.countBySheetTeamRole(SHEET_ID, HOME_TEAM, LineupRole.STARTER))
            .thenReturn(MatchSheetService.MAX_STARTERS_PER_TEAM.toLong())
        assertFailsWith<LineupRoleLimitException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `lineup add rejects a 13th substitute`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, HOME_TEAM))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L))).thenReturn(Optional.empty())
        whenever(lineupRepo.countBySheetTeamRole(SHEET_ID, HOME_TEAM, LineupRole.SUBSTITUTE))
            .thenReturn(MatchSheetService.MAX_SUBSTITUTES_PER_TEAM.toLong())
        assertFailsWith<LineupRoleLimitException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.SUBSTITUTE))
        }
    }

    @Test
    fun `lineup add succeeds within the roster limit`() {
        editableSheet()
        whenever(playerRepo.findActiveById(7L)).thenReturn(player(7L, HOME_TEAM))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L))).thenReturn(Optional.empty())
        whenever(lineupRepo.countBySheetTeamRole(SHEET_ID, HOME_TEAM, LineupRole.STARTER))
            .thenReturn(5L)
        doReturn(entry(7L, HOME_TEAM, LineupRole.STARTER)).whenever(lineupRepo).save(any())

        val saved = service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))

        assertEquals(7L, saved.id.playerId)
        assertEquals(LineupRole.STARTER, saved.role)
        verify(lineupRepo).save(any())
    }

    @Test
    fun `promoting a substitute to starter respects the starter limit`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 7L)))
            .thenReturn(Optional.of(entry(7L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(lineupRepo.countBySheetTeamRole(SHEET_ID, HOME_TEAM, LineupRole.STARTER))
            .thenReturn(MatchSheetService.MAX_STARTERS_PER_TEAM.toLong())
        assertFailsWith<LineupRoleLimitException> {
            service.updateLineupEntry(MATCH_ID, 7L, LineupUpdateRequest(role = LineupRole.STARTER))
        }
    }

    @Test
    fun `editing a locked sheet is rejected`() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.LIVE))
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = true))
        assertFailsWith<SheetLockedException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `editing a sheet of a finished match is rejected`() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.FINISHED))
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = false))
        assertFailsWith<SheetNotEditableException> {
            service.addLineupEntry(MATCH_ID, lineupReq(7L, LineupRole.STARTER))
        }
    }

    @Test
    fun `occurrence author must be in the lineup`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L))).thenReturn(Optional.empty())
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(MATCH_ID, occReq(OccurrenceType.GOAL, playerId = 10L))
        }
    }

    @Test
    fun `a player sent off cannot author further occurrences`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(true)
        assertFailsWith<PlayerSentOffException> {
            service.addOccurrence(MATCH_ID, occReq(OccurrenceType.GOAL, playerId = 10L))
        }
    }

    @Test
    fun `a foul is a valid occurrence`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        doReturn(occurrence(OccurrenceType.FOUL, playerId = 10L, teamId = HOME_TEAM))
            .whenever(occurrenceRepo).save(any())

        val occ = service.addOccurrence(MATCH_ID, occReq(OccurrenceType.FOUL, playerId = 10L))

        assertEquals(OccurrenceType.FOUL, occ.type)
        assertEquals(HOME_TEAM, occ.teamId)
    }

    @Test
    fun `replacedPlayerId is only allowed on substitutions`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(
                MATCH_ID,
                occReq(OccurrenceType.GOAL, playerId = 10L, replacedPlayerId = 11L),
            )
        }
    }

    @Test
    fun `substitution requires a replaced player`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(MATCH_ID, occReq(OccurrenceType.SUBSTITUTION, playerId = 10L))
        }
    }

    @Test
    fun `substitution rejects same player on and off`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(
                MATCH_ID,
                occReq(OccurrenceType.SUBSTITUTION, playerId = 10L, replacedPlayerId = 10L),
            )
        }
    }

    @Test
    fun `substitution requires the replaced player to be in the lineup`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 11L))).thenReturn(Optional.empty())
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(
                MATCH_ID,
                occReq(OccurrenceType.SUBSTITUTION, playerId = 10L, replacedPlayerId = 11L),
            )
        }
    }

    @Test
    fun `substitution requires both players on the same team`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 11L)))
            .thenReturn(Optional.of(entry(11L, AWAY_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        assertFailsWith<IllegalArgumentException> {
            service.addOccurrence(
                MATCH_ID,
                occReq(OccurrenceType.SUBSTITUTION, playerId = 10L, replacedPlayerId = 11L),
            )
        }
    }

    @Test
    fun `a 6th substitution for a team is rejected`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 11L)))
            .thenReturn(Optional.of(entry(11L, HOME_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        whenever(occurrenceRepo.countBySheetTeamType(SHEET_ID, HOME_TEAM, OccurrenceType.SUBSTITUTION))
            .thenReturn(MatchSheetService.MAX_SUBSTITUTIONS_PER_TEAM)
        assertFailsWith<TooManySubstitutionsException> {
            service.addOccurrence(
                MATCH_ID,
                occReq(OccurrenceType.SUBSTITUTION, playerId = 10L, replacedPlayerId = 11L),
            )
        }
    }

    @Test
    fun `a valid substitution within the limit is accepted`() {
        editableSheet()
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.SUBSTITUTE)))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 11L)))
            .thenReturn(Optional.of(entry(11L, HOME_TEAM, LineupRole.STARTER)))
        whenever(occurrenceRepo.playerHasRedCard(SHEET_ID, 10L)).thenReturn(false)
        whenever(occurrenceRepo.countBySheetTeamType(SHEET_ID, HOME_TEAM, OccurrenceType.SUBSTITUTION))
            .thenReturn(2L)
        doReturn(
            occurrence(OccurrenceType.SUBSTITUTION, playerId = 10L, teamId = HOME_TEAM, replaced = 11L),
        ).whenever(occurrenceRepo).save(any())

        val occ = service.addOccurrence(
            MATCH_ID,
            occReq(OccurrenceType.SUBSTITUTION, playerId = 10L, replacedPlayerId = 11L),
        )

        assertEquals(OccurrenceType.SUBSTITUTION, occ.type)
        assertEquals(11L, occ.replacedPlayerId)
    }

    @Test
    fun `lock marks the sheet locked and publishes the closed event`() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.FINISHED))
        val sheet = sheet(locked = false)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet)

        val locked = service.lock(MATCH_ID)

        assertTrue(locked.locked)
        verify(publisher).publishMatchSheetClosed(sheet)
    }

    @Test
    fun `locking an already locked sheet still publishes (idempotent)`() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.FINISHED))
        val sheet = sheet(locked = true)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet)

        service.lock(MATCH_ID)

        verify(publisher).publishMatchSheetClosed(sheet)
    }

    @Test
    fun `unlock clears the locked flag and does not publish`() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.LIVE))
        val sheet = sheet(locked = true)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet)

        val unlocked = service.unlock(MATCH_ID)

        assertTrue(!unlocked.locked)
        verify(publisher, never()).publishMatchSheetClosed(any())
    }

    @Test
    fun `autoLock is a no-op when no sheet exists`() {
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(null)

        service.autoLockIfPresent(MATCH_ID)

        verify(publisher, never()).publishMatchSheetClosed(any())
    }

    @Test
    fun `autoLock locks an existing sheet and publishes`() {
        val sheet = sheet(locked = false)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet)

        service.autoLockIfPresent(MATCH_ID)

        assertTrue(sheet.locked)
        verify(publisher).publishMatchSheetClosed(sheet)
    }

    @Test
    fun `adding a goal syncs the match score`() {
        val m = match(MatchStatus.SCHEDULED)
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(m)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = false))
        whenever(lineupRepo.findById(LineupEntryId(SHEET_ID, 10L)))
            .thenReturn(Optional.of(entry(10L, HOME_TEAM, LineupRole.STARTER)))
        doReturn(occurrence(OccurrenceType.GOAL, playerId = 10L, teamId = HOME_TEAM))
            .whenever(occurrenceRepo).save(any())
        whenever(occurrenceRepo.findBySheet(SHEET_ID))
            .thenReturn(listOf(occurrence(OccurrenceType.GOAL, playerId = 10L, teamId = HOME_TEAM)))

        service.addOccurrence(MATCH_ID, occReq(OccurrenceType.GOAL, playerId = 10L))

        assertEquals(1, m.homeScore)
        assertEquals(0, m.awayScore)
    }

    @Test
    fun `closing the sheet finishes the match with the score from the sheet`() {
        val m = match(MatchStatus.SCHEDULED)
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(m)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = false))
        whenever(occurrenceRepo.findBySheet(SHEET_ID)).thenReturn(
            listOf(
                occurrence(OccurrenceType.GOAL, playerId = 10L, teamId = HOME_TEAM),
                occurrence(OccurrenceType.GOAL, playerId = 11L, teamId = HOME_TEAM),
                occurrence(OccurrenceType.OWN_GOAL, playerId = 20L, teamId = AWAY_TEAM),
            ),
        )

        service.lock(MATCH_ID)

        assertEquals(MatchStatus.FINISHED, m.status)
        assertEquals(3, m.homeScore)
        assertEquals(0, m.awayScore)
    }

    @Test
    fun `reopening a finished match makes it editable again`() {
        val m = match(MatchStatus.FINISHED)
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(m)
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = true))

        service.unlock(MATCH_ID)

        assertEquals(MatchStatus.SCHEDULED, m.status)
    }

    private fun editableSheet() {
        whenever(matchRepo.findActiveById(MATCH_ID)).thenReturn(match(MatchStatus.SCHEDULED))
        whenever(sheetRepo.findByMatchId(MATCH_ID)).thenReturn(sheet(locked = false))
    }

    private fun match(status: MatchStatus) = Match(
        id = MATCH_ID,
        homeTeamId = HOME_TEAM,
        awayTeamId = AWAY_TEAM,
        kickoffAt = OffsetDateTime.now().plusDays(1),
        status = status,
    )

    private fun sheet(locked: Boolean) = MatchSheet(id = SHEET_ID, matchId = MATCH_ID, locked = locked)

    private fun player(id: Long, teamId: Long, shirt: Int? = null) = Player(
        id = id,
        teamId = teamId,
        firstName = "Test",
        lastName = "Player",
        position = PlayerPosition.MF,
        shirtNumber = shirt,
        createdAt = OffsetDateTime.now(),
    )

    private fun entry(playerId: Long, teamId: Long, role: LineupRole) = LineupEntry(
        id = LineupEntryId(SHEET_ID, playerId),
        teamId = teamId,
        role = role,
    )

    private fun occurrence(
        type: OccurrenceType,
        playerId: Long,
        teamId: Long,
        replaced: Long? = null,
    ) = Occurrence(
        id = 1L,
        matchSheetId = SHEET_ID,
        minute = 25,
        type = type,
        teamId = teamId,
        playerId = playerId,
        replacedPlayerId = replaced,
    )

    private fun lineupReq(playerId: Long, role: LineupRole) =
        LineupCreateRequest(playerId = playerId, role = role)

    private fun occReq(
        type: OccurrenceType,
        playerId: Long,
        replacedPlayerId: Long? = null,
    ) = OccurrenceCreateRequest(
        minute = 25,
        type = type,
        playerId = playerId,
        replacedPlayerId = replacedPlayerId,
    )
}
