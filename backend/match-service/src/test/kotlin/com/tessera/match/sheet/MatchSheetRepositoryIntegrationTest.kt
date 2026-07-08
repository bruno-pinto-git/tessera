package com.tessera.match.sheet

import com.tessera.match.club.Club
import com.tessera.match.match.Match
import com.tessera.match.match.MatchStatus
import com.tessera.match.player.Player
import com.tessera.match.player.PlayerPosition
import com.tessera.match.team.Team
import com.tessera.match.team.TeamCategory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class MatchSheetRepositoryIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired private lateinit var em: TestEntityManager
    @Autowired private lateinit var lineupRepo: LineupEntryRepository
    @Autowired private lateinit var occurrenceRepo: OccurrenceRepository

    private var teamId = 0L
    private var sheetId = 0L
    private val players = mutableMapOf<Int, Long>()

    @BeforeEach
    fun setup() {
        val club = em.persistAndFlush(Club(name = "Sporting"))
        val home = em.persistAndFlush(Team(clubId = club.id, category = TeamCategory.SENIOR_M))
        val away = em.persistAndFlush(Team(clubId = club.id, category = TeamCategory.SENIOR_F))
        teamId = home.id
        (1..4).forEach { n ->
            val p = em.persistAndFlush(
                Player(teamId = home.id, firstName = "P$n", lastName = "L", position = PlayerPosition.MF, shirtNumber = n),
            )
            players[n] = p.id
        }
        val match = em.persistAndFlush(
            Match(homeTeamId = home.id, awayTeamId = away.id, kickoffAt = OffsetDateTime.now().plusDays(1), status = MatchStatus.SCHEDULED),
        )
        sheetId = em.persistAndFlush(MatchSheet(matchId = match.id)).id
    }

    @Test
    fun `countBySheetTeamRole counts entries per role`() {
        starter(shirt = 1)
        starter(shirt = 2)
        starter(shirt = 3)
        substitute(shirt = 4)

        assertEquals(3, lineupRepo.countBySheetTeamRole(sheetId, teamId, LineupRole.STARTER))
        assertEquals(1, lineupRepo.countBySheetTeamRole(sheetId, teamId, LineupRole.SUBSTITUTE))
    }

    @Test
    fun `shirt-number existence queries respect the excluded player`() {
        starter(shirt = 1)

        assertTrue(lineupRepo.existsBySheetTeamShirt(sheetId, teamId, 1))
        assertFalse(lineupRepo.existsBySheetTeamShirt(sheetId, teamId, 99))
        assertFalse(lineupRepo.existsBySheetTeamShirtExcluding(sheetId, teamId, 1, players.getValue(1)))
        assertTrue(lineupRepo.existsBySheetTeamShirtExcluding(sheetId, teamId, 1, players.getValue(2)))
    }

    @Test
    fun `playerHasRedCard detects a sent-off player only`() {
        occurrence(OccurrenceType.RED_CARD, minute = 40, playerShirt = 1)

        assertTrue(occurrenceRepo.playerHasRedCard(sheetId, players.getValue(1)))
        assertFalse(occurrenceRepo.playerHasRedCard(sheetId, players.getValue(2)))
    }

    @Test
    fun `countBySheetTeamType counts substitutions for the team`() {
        substitution(minute = 60, onShirt = 4, offShirt = 1)
        substitution(minute = 65, onShirt = 3, offShirt = 2)

        assertEquals(2, occurrenceRepo.countBySheetTeamType(sheetId, teamId, OccurrenceType.SUBSTITUTION))
        assertEquals(0, occurrenceRepo.countBySheetTeamType(sheetId, teamId, OccurrenceType.GOAL))
    }

    private fun starter(shirt: Int) = lineupEntry(shirt, LineupRole.STARTER)
    private fun substitute(shirt: Int) = lineupEntry(shirt, LineupRole.SUBSTITUTE)

    private fun lineupEntry(shirt: Int, role: LineupRole) {
        em.persistAndFlush(
            LineupEntry(
                id = LineupEntryId(matchSheetId = sheetId, playerId = players.getValue(shirt)),
                teamId = teamId,
                shirtNumber = shirt,
                role = role,
            ),
        )
    }

    private fun occurrence(type: OccurrenceType, minute: Int, playerShirt: Int, replacedShirt: Int? = null) {
        em.persistAndFlush(
            Occurrence(
                matchSheetId = sheetId,
                minute = minute,
                type = type,
                teamId = teamId,
                playerId = players.getValue(playerShirt),
                replacedPlayerId = replacedShirt?.let { players.getValue(it) },
            ),
        )
    }

    private fun substitution(minute: Int, onShirt: Int, offShirt: Int) =
        occurrence(OccurrenceType.SUBSTITUTION, minute, playerShirt = onShirt, replacedShirt = offShirt)
}
