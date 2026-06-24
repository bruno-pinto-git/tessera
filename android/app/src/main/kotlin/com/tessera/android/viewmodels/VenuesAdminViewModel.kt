package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.VenueRepository
import com.tessera.android.data.dto.VenueDto
import kotlinx.coroutines.launch

sealed interface AdminVenuesState {
    data object Loading : AdminVenuesState
    data class Success(val venues: List<VenueDto>) : AdminVenuesState
    data object Error : AdminVenuesState
}

class VenuesAdminViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = VenueRepository(application)

    var state by mutableStateOf<AdminVenuesState>(AdminVenuesState.Loading)
        private set
    var busy by mutableStateOf(false)
        private set

    init {
        load()
    }

    fun load() {
        state = AdminVenuesState.Loading
        viewModelScope.launch {
            state = try {
                AdminVenuesState.Success(repo.list())
            } catch (e: Exception) {
                AdminVenuesState.Error
            }
        }
    }

    fun createVenue(name: String, capacity: Int, address: String?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.create(name, capacity, address)
                state = AdminVenuesState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminVenuesState.Error
            }
            busy = false
        }
    }

    fun deleteVenue(id: Long) {
        busy = true
        viewModelScope.launch {
            try {
                repo.delete(id)
                state = AdminVenuesState.Success(repo.list())
            } catch (e: Exception) {
                state = AdminVenuesState.Error
            }
            busy = false
        }
    }
}
