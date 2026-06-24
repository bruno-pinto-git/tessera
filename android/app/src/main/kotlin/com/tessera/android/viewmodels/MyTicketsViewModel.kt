package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.EventCatalogRepository
import com.tessera.android.data.TicketRepository
import com.tessera.android.data.dto.CatalogEntry
import com.tessera.android.data.dto.TicketDto
import kotlinx.coroutines.launch
import kotlin.math.abs

data class TicketView(
    val ticket: TicketDto,
    val entry: CatalogEntry?,
    val supporter: Boolean,
)

sealed interface MyTicketsState {
    data object Loading : MyTicketsState
    data class Success(
        val paid: List<TicketView>,
        val pending: List<TicketView>,
        val past: List<TicketView>,
    ) : MyTicketsState
    data object Error : MyTicketsState
}

class MyTicketsViewModel(application: Application) : AndroidViewModel(application) {

    private val tickets = TicketRepository(application)
    private val catalog = EventCatalogRepository(application)

    var state by mutableStateOf<MyTicketsState>(MyTicketsState.Loading)
        private set

    init {
        load()
    }

    fun load() {
        state = MyTicketsState.Loading
        viewModelScope.launch {
            state = try {
                val mine = tickets.mine()
                val byEvent = runCatching { catalog.catalog() }.getOrDefault(emptyList()).associateBy { it.eventId }
                val views = mine.map { t ->
                    val e = byEvent[t.eventId]
                    TicketView(t, e, supporter = isSupporter(t, e))
                }
                MyTicketsState.Success(
                    paid = views.filter { it.ticket.status == "PAID" }
                        .sortedWith(compareBy(nullsLast()) { it.entry?.kickoffAt }),
                    pending = views.filter { it.ticket.status == "PENDING" }
                        .sortedWith(compareBy(nullsLast()) { it.entry?.kickoffAt }),
                    past = views.filter { it.ticket.status == "VALIDATED" }
                        .sortedWith(compareByDescending { it.entry?.kickoffAt ?: "" }),
                )
            } catch (e: Exception) {
                MyTicketsState.Error
            }
        }
    }

    private fun isSupporter(t: TicketDto, e: CatalogEntry?): Boolean =
        e != null && abs(t.price - e.priceSupporter) < abs(t.price - e.priceNormal)
}
