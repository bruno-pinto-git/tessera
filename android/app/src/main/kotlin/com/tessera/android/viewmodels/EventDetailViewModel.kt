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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PurchaseStep { TIER, METHOD, DONE }

enum class PurchaseError { INVALID_PHONE, PAYMENT_FAILED }

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val catalog = EventCatalogRepository(application)
    private val tickets = TicketRepository(application)

    var entry by mutableStateOf<CatalogEntry?>(null)
        private set
    var loadError by mutableStateOf(false)
        private set

    var sheetOpen by mutableStateOf(false)
        private set
    var step by mutableStateOf(PurchaseStep.TIER)
        private set
    var supporter by mutableStateOf(false)
        private set
    var method by mutableStateOf("MBWAY")
        private set
    var phone by mutableStateOf("")
        private set
    var ticket by mutableStateOf<TicketDto?>(null)
        private set
    var formError by mutableStateOf<PurchaseError?>(null)
        private set
    var submitting by mutableStateOf(false)
        private set
    var awaiting by mutableStateOf(false)
        private set

    val total: Double
        get() = entry?.let { if (supporter) it.priceSupporter else it.priceNormal } ?: 0.0

    fun load(id: Long) {
        loadError = false
        entry = null
        viewModelScope.launch {
            try {
                entry = catalog.entry(id)
            } catch (e: Exception) {
                loadError = true
            }
        }
    }

    fun openPurchase() {
        step = PurchaseStep.TIER
        supporter = false
        method = "MBWAY"
        phone = ""
        ticket = null
        formError = null
        submitting = false
        awaiting = false
        sheetOpen = true
    }

    fun dismiss() {
        sheetOpen = false
    }

    fun selectTier(isSupporter: Boolean) {
        supporter = isSupporter
    }

    fun toMethod() {
        step = PurchaseStep.METHOD
    }

    fun backToTier() {
        formError = null
        step = PurchaseStep.TIER
    }

    fun selectMethod(m: String) {
        method = m
        formError = null
    }

    fun updatePhone(p: String) {
        phone = p
        formError = null
    }

    fun confirm() {
        val e = entry ?: return
        formError = null
        val normalized = phone.replace(Regex("[\\s+]"), "")
        if (method == "MBWAY" && !Regex("^\\d{9,15}$").matches(normalized)) {
            formError = PurchaseError.INVALID_PHONE
            return
        }
        submitting = true
        viewModelScope.launch {
            try {
                val created = tickets.create(e.eventId, supporter)
                val paid = tickets.pay(created.id, method, if (method == "MBWAY") normalized else null)
                when {
                    method == "MBWAY" && paid.status != "PAID" -> {
                        submitting = false
                        awaiting = true
                        val confirmed = pollUntilPaid(created.id)
                        if (confirmed != null && confirmed.status == "PAID") {
                            ticket = confirmed
                            step = PurchaseStep.DONE
                        } else {
                            formError = PurchaseError.PAYMENT_FAILED
                        }
                    }
                    paid.status == "PAID" -> {
                        ticket = paid
                        step = PurchaseStep.DONE
                    }
                    else -> formError = PurchaseError.PAYMENT_FAILED
                }
            } catch (ex: Exception) {
                formError = PurchaseError.PAYMENT_FAILED
            } finally {
                submitting = false
                awaiting = false
            }
        }
    }

    private suspend fun pollUntilPaid(id: Long): TicketDto? {
        repeat(POLL_ATTEMPTS) {
            val t = tickets.get(id)
            if (t.status == "PAID" || t.status == "CANCELLED") return t
            delay(POLL_INTERVAL_MS)
        }
        return null
    }

    private companion object {
        const val POLL_ATTEMPTS = 90
        const val POLL_INTERVAL_MS = 2000L
    }
}
