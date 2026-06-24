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

sealed interface ClubsState {
    data object Loading : ClubsState
    data class Success(val clubs: List<ClubDto>) : ClubsState
    data object Error : ClubsState
}

class ClubsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ClubRepository(application)

    var state by mutableStateOf<ClubsState>(ClubsState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = ClubsState.Loading
        viewModelScope.launch {
            state = try {
                val me = repo.me()
                val clubIds = me.clubMemberships.map { it.clubId }.distinct()
                ClubsState.Success(clubIds.map { repo.getClub(it) })
            } catch (e: Exception) {
                ClubsState.Error
            }
        }
    }
}
