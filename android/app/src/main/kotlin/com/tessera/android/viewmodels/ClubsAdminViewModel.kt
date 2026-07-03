package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.ClubRepository
import com.tessera.android.data.dto.ClubDto
import kotlinx.coroutines.launch

sealed interface AdminClubsState {
    data object Loading : AdminClubsState
    data class Success(val clubs: List<ClubDto>) : AdminClubsState
    data object Error : AdminClubsState
}

class ClubsAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ClubRepository(application)

    var state by mutableStateOf<AdminClubsState>(AdminClubsState.Loading)
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = AdminClubsState.Loading
        viewModelScope.launch {
            state = try {
                AdminClubsState.Success(repo.listClubs())
            } catch (e: Exception) {
                AdminClubsState.Error
            }
        }
    }

    fun createClub(name: String, foundedYear: Int?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.createClub(name, foundedYear, null)
                state = AdminClubsState.Success(repo.listClubs())
            } catch (e: Exception) {
                state = AdminClubsState.Error
            }
            busy = false
        }
    }

    fun updateClub(id: Long, name: String, foundedYear: Int?, crestUrl: String?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updateClub(id, name, foundedYear, crestUrl)
                state = AdminClubsState.Success(repo.listClubs())
            } catch (e: Exception) {
                state = AdminClubsState.Error
            }
            busy = false
        }
    }

    fun deleteClub(id: Long) {
        busy = true
        viewModelScope.launch {
            try {
                repo.deleteClub(id)
                state = AdminClubsState.Success(repo.listClubs())
            } catch (e: Exception) {
                state = AdminClubsState.Error
            }
            busy = false
        }
    }
}
