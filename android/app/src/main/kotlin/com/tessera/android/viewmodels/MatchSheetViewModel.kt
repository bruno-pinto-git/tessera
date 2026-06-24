package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.ClubRepository
import com.tessera.android.data.SheetRepository
import com.tessera.android.data.TeamRepository
import com.tessera.android.data.dto.LineupEntryDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.MatchSheetDto
import com.tessera.android.data.dto.PlayerDto
import kotlinx.coroutines.launch

class MatchSheetViewModel(application: Application) : AndroidViewModel(application) {

    private val sheets = SheetRepository(application)
    private val teams = TeamRepository(application)
    private val clubs = ClubRepository(application)

    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var match by mutableStateOf<MatchDto?>(null)
        private set
    var sheet by mutableStateOf<MatchSheetDto?>(null)
        private set
    var homeName by mutableStateOf("")
        private set
    var awayName by mutableStateOf("")
        private set
    var homeRoster by mutableStateOf<List<PlayerDto>>(emptyList())
        private set
    var awayRoster by mutableStateOf<List<PlayerDto>>(emptyList())
        private set

    private var playersById: Map<Long, PlayerDto> = emptyMap()
    private var currentMatchId: Long = 0

    val locked: Boolean get() = sheet?.locked == true

    fun load(matchId: Long) {
        currentMatchId = matchId
        loading = true
        error = false
        viewModelScope.launch {
            try {
                val m = sheets.getMatch(matchId)
                match = m
                homeRoster = runCatching { teams.players(m.homeTeamId) }.getOrDefault(emptyList())
                awayRoster = runCatching { teams.players(m.awayTeamId) }.getOrDefault(emptyList())
                playersById = (homeRoster + awayRoster).associateBy { it.id }
                homeName = m.homeClubId?.let { runCatching { clubs.getClub(it).name }.getOrNull() } ?: "Casa"
                awayName = m.awayClubId?.let { runCatching { clubs.getClub(it).name }.getOrNull() } ?: "Fora"
                sheet = sheets.getSheet(matchId)
            } catch (e: Exception) {
                error = true
            }
            loading = false
        }
    }

    fun playerName(id: Long): String =
        playersById[id]?.let { "${it.firstName} ${it.lastName}" } ?: "Jogador #$id"

    fun lineupFor(teamId: Long, role: String): List<LineupEntryDto> =
        sheet?.lineup?.filter { it.teamId == teamId && it.role == role }?.sortedBy { it.shirtNumber ?: 99 } ?: emptyList()

    /** Jogadores da equipa ainda não convocados. */
    fun availableFor(teamId: Long, roster: List<PlayerDto>): List<PlayerDto> {
        val inLineup = sheet?.lineup?.map { it.playerId }?.toSet() ?: emptySet()
        return roster.filter { it.id !in inLineup }
    }

    /** Convocados de uma equipa (para escolher em lances). */
    fun calledUp(teamId: Long): List<LineupEntryDto> =
        sheet?.lineup?.filter { it.teamId == teamId } ?: emptyList()

    private fun refresh() {
        viewModelScope.launch {
            runCatching { sheet = sheets.getSheet(currentMatchId) }
            busy = false
        }
    }

    fun addLineup(playerId: Long, role: String, shirtNumber: Int?) {
        busy = true
        viewModelScope.launch {
            try {
                sheets.addLineup(currentMatchId, playerId, role, shirtNumber)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun removeLineup(playerId: Long) {
        busy = true
        viewModelScope.launch {
            try {
                sheets.removeLineup(currentMatchId, playerId)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun addOccurrence(minute: Int, type: String, playerId: Long, replacedPlayerId: Long?) {
        busy = true
        viewModelScope.launch {
            try {
                sheets.addOccurrence(currentMatchId, minute, type, playerId, replacedPlayerId)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun removeOccurrence(occurrenceId: Long) {
        busy = true
        viewModelScope.launch {
            try {
                sheets.removeOccurrence(currentMatchId, occurrenceId)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun lock() {
        busy = true
        viewModelScope.launch {
            try {
                sheets.lock(currentMatchId)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun unlock() {
        busy = true
        viewModelScope.launch {
            try {
                sheets.unlock(currentMatchId)
                sheet = sheets.getSheet(currentMatchId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }
}
