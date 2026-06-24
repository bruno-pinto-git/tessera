package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.TeamRepository
import com.tessera.android.data.dto.PlayerDto
import com.tessera.android.data.dto.PlayerInput
import com.tessera.android.data.dto.TeamDto
import kotlinx.coroutines.launch

class TeamDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TeamRepository(application)

    var team by mutableStateOf<TeamDto?>(null)
        private set
    var players by mutableStateOf<List<PlayerDto>>(emptyList())
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set

    private var currentTeamId: Long = 0

    fun load(teamId: Long) {
        currentTeamId = teamId
        loading = true
        error = false
        viewModelScope.launch {
            try {
                team = repo.getTeam(teamId)
                players = repo.players(teamId)
            } catch (e: Exception) {
                error = true
            }
            loading = false
        }
    }

    fun createPlayer(input: PlayerInput) {
        busy = true
        viewModelScope.launch {
            try {
                repo.createPlayer(currentTeamId, input)
                players = repo.players(currentTeamId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun updatePlayer(playerId: Long, input: PlayerInput) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updatePlayer(playerId, input)
                players = repo.players(currentTeamId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun deletePlayer(playerId: Long) {
        busy = true
        viewModelScope.launch {
            try {
                repo.deletePlayer(playerId)
                players = repo.players(currentTeamId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }
}
