package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.EventCatalogRepository
import com.tessera.android.data.dto.CatalogEntry
import kotlinx.coroutines.launch

enum class EventsFilter { ALL, UPCOMING, FINISHED }

sealed interface EventsState {
    data object Loading : EventsState
    data class Success(val all: List<CatalogEntry>) : EventsState
    data object Error : EventsState
}

class EventsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = EventCatalogRepository(app.applicationContext)

    var state by mutableStateOf<EventsState>(EventsState.Loading)
        private set
    var filter by mutableStateOf(EventsFilter.ALL)
        private set

    init {
        load()
    }

    fun load() {
        state = EventsState.Loading
        viewModelScope.launch {
            state = try {
                EventsState.Success(repo.catalog().filter { it.eventStatus == "PUBLISHED" })
            } catch (e: Exception) {
                EventsState.Error
            }
        }
    }

    fun updateFilter(f: EventsFilter) {
        filter = f
    }

    fun visible(all: List<CatalogEntry>): List<CatalogEntry> =
        all.filter { matchesFilter(it, filter) }

    private fun matchesFilter(e: CatalogEntry, f: EventsFilter): Boolean = when (f) {
        EventsFilter.ALL -> true
        EventsFilter.UPCOMING -> e.matchStatus == null || e.matchStatus in UPCOMING_STATUSES
        EventsFilter.FINISHED -> e.matchStatus in FINISHED_STATUSES
    }

    private companion object {
        val UPCOMING_STATUSES = setOf("SCHEDULED", "LIVE", "POSTPONED")
        val FINISHED_STATUSES = setOf("FINISHED", "ABANDONED")
    }
}
