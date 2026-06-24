package com.tessera.android.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.CatalogEntry
import com.tessera.android.screens.components.ClubCrest
import com.tessera.android.screens.components.StatusBadge
import com.tessera.android.screens.components.QrView
import com.tessera.android.screens.components.formatCapacity
import com.tessera.android.screens.components.formatEur
import com.tessera.android.screens.components.formatKickoffFull
import com.tessera.android.screens.components.prettyEventStatus
import com.tessera.android.shared.AuthSession
import com.tessera.android.viewmodels.EventDetailViewModel
import com.tessera.android.viewmodels.PurchaseError
import com.tessera.android.viewmodels.PurchaseStep

private val ENDED_STATUSES = setOf("FINISHED", "ABANDONED", "CANCELLED")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: Long,
    onOpenTickets: () -> Unit = {},
    onRequestLogin: () -> Unit = {},
    viewModel: EventDetailViewModel = viewModel(),
) {
    LaunchedEffect(eventId) { viewModel.load(eventId) }

    val entry = viewModel.entry
    when {
        viewModel.loadError -> Centered {
            Text(stringResource(R.string.event_detail_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.load(eventId) }) { Text(stringResource(R.string.common_retry)) }
        }
        entry == null -> Centered { CircularProgressIndicator() }
        else -> EventDetailContent(entry, viewModel, onRequestLogin)
    }

    if (viewModel.sheetOpen) {
        Dialog(onDismissRequest = viewModel::dismiss) {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                PurchaseSheet(entry = viewModel.entry, viewModel = viewModel, onOpenTickets = onOpenTickets)
            }
        }
    }
}

@Composable
private fun EventDetailContent(entry: CatalogEntry, viewModel: EventDetailViewModel, onRequestLogin: () -> Unit) {
    val ended = entry.matchStatus in ENDED_STATUSES
    val saleable = entry.eventStatus == "PUBLISHED" && !ended
    val authenticated = AuthSession.isAuthenticated
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderCard(entry)
            DetailsCard(entry)
            PriceTableCard(entry)
            TipsCard()
        }
        PurchaseBar(
            entry = entry,
            saleable = saleable,
            ended = ended,
            authenticated = authenticated,
            onBuy = viewModel::openPurchase,
            onRequestLogin = onRequestLogin,
        )
    }
}

@Composable
private fun HeaderCard(entry: CatalogEntry) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CrestBig(entry.homeInitials, entry.homeShort, stringResource(R.string.match_home), entry.homeTone, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.hasResult) {
                    Text("${entry.homeScore} – ${entry.awayScore}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Text(stringResource(R.string.match_vs), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusBadge(entry.matchStatus ?: entry.eventStatus)
            }
            CrestBig(entry.awayInitials, entry.awayShort, stringResource(R.string.match_away), entry.awayTone, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CrestBig(initials: String, short: String, label: String, tone: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ClubCrest(initials, tone, 72.dp)
        Text(short, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, maxLines = 2)
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailsCard(entry: CatalogEntry) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoBlock(stringResource(R.string.ed_data), formatKickoffFull(entry.kickoffAt), null)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoBlock(
                stringResource(R.string.ed_venue),
                entry.venueName ?: stringResource(R.string.ed_venue_tbd),
                entry.venueCapacity?.let { formatCapacity(it) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoBlock(
                stringResource(R.string.ed_box_office),
                prettyEventStatus(entry.eventStatus),
                stringResource(R.string.ed_box_office_prices, formatEur(entry.priceNormal), formatEur(entry.priceSupporter)),
            )
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String, sub: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun PriceTableCard(entry: CatalogEntry) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ed_price_table_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            PriceRow(stringResource(R.string.ed_price_normal_title), stringResource(R.string.ed_price_normal_sub), entry.priceNormal, false)
            PriceRow(stringResource(R.string.ed_price_supporter_title), stringResource(R.string.ed_price_supporter_sub), entry.priceSupporter, true)
        }
    }
}

@Composable
private fun PriceRow(title: String, subtitle: String, price: Double, highlight: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            formatEur(price),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TipsCard() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.ed_tips_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            TipRow(stringResource(R.string.ed_tip_policy), stringResource(R.string.ed_tip_policy_v))
        }
    }
}

@Composable
private fun TipRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PurchaseBar(
    entry: CatalogEntry,
    saleable: Boolean,
    ended: Boolean,
    authenticated: Boolean,
    onBuy: () -> Unit,
    onRequestLogin: () -> Unit,
) {
    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.events_from), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatEur(entry.priceFrom), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = { if (authenticated) onBuy() else onRequestLogin() },
                enabled = saleable,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            ) {
                Text(
                    when {
                        ended -> stringResource(R.string.purchase_ended)
                        entry.eventStatus != "PUBLISHED" -> prettyEventStatus(entry.eventStatus)
                        !authenticated -> stringResource(R.string.purchase_login_to_buy)
                        else -> stringResource(R.string.purchase_buy)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// --- Fluxo de compra ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseSheet(entry: CatalogEntry?, viewModel: EventDetailViewModel, onOpenTickets: () -> Unit) {
    entry ?: return
    val busy = viewModel.submitting || viewModel.awaiting
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.purchase_eyebrow).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "${entry.homeShort} ${stringResource(R.string.match_vs)} ${entry.awayShort}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Stepper(currentIndex = viewModel.step.ordinal)

        when (viewModel.step) {
            PurchaseStep.TIER -> TierStep(entry, viewModel)
            PurchaseStep.METHOD -> MethodStep(viewModel, busy)
            PurchaseStep.DONE -> DoneStep(viewModel, onOpenTickets)
        }
    }
}

@Composable
private fun Stepper(currentIndex: Int) {
    val labels = listOf(
        stringResource(R.string.step_seat),
        stringResource(R.string.step_payment),
        stringResource(R.string.step_qr),
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { i, label ->
            StepDot(number = i + 1, label = label, active = currentIndex >= i, done = currentIndex > i)
            if (i < labels.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    color = if (currentIndex > i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun StepDot(number: Int, label: String, active: Boolean, done: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier.size(28.dp).background(
                if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape,
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            } else {
                Text("$number", color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TierStep(entry: CatalogEntry, viewModel: EventDetailViewModel) {
    SelectableOption(
        title = stringResource(R.string.price_normal),
        subtitle = stringResource(R.string.tier_normal_sub),
        trailing = formatEur(entry.priceNormal),
        selected = !viewModel.supporter,
    ) { viewModel.selectTier(false) }
    SelectableOption(
        title = stringResource(R.string.price_supporter),
        subtitle = stringResource(R.string.tier_supporter_sub),
        trailing = formatEur(entry.priceSupporter),
        selected = viewModel.supporter,
    ) { viewModel.selectTier(true) }
    Button(onClick = viewModel::toMethod, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) {
        Text(stringResource(R.string.purchase_continue))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodStep(viewModel: EventDetailViewModel, busy: Boolean) {
    SelectableOption(stringResource(R.string.method_mbway), stringResource(R.string.method_mbway_sub), null, viewModel.method == "MBWAY", enabled = !busy) { viewModel.selectMethod("MBWAY") }
    SelectableOption(stringResource(R.string.method_paid_test), stringResource(R.string.method_paid_test_sub), null, viewModel.method == "CARD", enabled = !busy) { viewModel.selectMethod("CARD") }

    if (viewModel.method == "MBWAY") {
        OutlinedTextField(
            value = viewModel.phone,
            onValueChange = viewModel::updatePhone,
            label = { Text(stringResource(R.string.purchase_phone_mbway_label)) },
            placeholder = { Text(stringResource(R.string.purchase_phone_mbway_ph)) },
            singleLine = true,
            enabled = !busy,
            isError = viewModel.formError == PurchaseError.INVALID_PHONE,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.purchase_phone_helper), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SummaryBox(viewModel)

    viewModel.formError?.let {
        val msg = when (it) {
            PurchaseError.INVALID_PHONE -> stringResource(R.string.purchase_phone_invalid)
            PurchaseError.PAYMENT_FAILED -> stringResource(R.string.purchase_failed)
        }
        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::backToTier, enabled = !busy, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text(stringResource(R.string.purchase_back))
        }
        Button(onClick = viewModel::confirm, enabled = !busy, modifier = Modifier.weight(1f), contentPadding = PaddingValues(vertical = 14.dp)) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(if (viewModel.awaiting) R.string.purchase_awaiting_mbway else R.string.purchase_processing))
            } else {
                Text(stringResource(R.string.purchase_confirm))
            }
        }
    }
}

@Composable
private fun SummaryBox(viewModel: EventDetailViewModel) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryRow(stringResource(R.string.purchase_summary_seat), stringResource(if (viewModel.supporter) R.string.price_supporter else R.string.price_normal))
            SummaryRow(stringResource(R.string.purchase_summary_total), formatEur(viewModel.total))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DoneStep(viewModel: EventDetailViewModel, onOpenTickets: () -> Unit) {
    val ticket = viewModel.ticket
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusBadge("PAID")
        if (ticket != null) {
            QrView(content = ticket.code, modifier = Modifier.size(200.dp))
            Text(truncateCode(ticket.code), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(stringResource(if (viewModel.supporter) R.string.price_supporter else R.string.price_normal), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Button(
            onClick = { viewModel.dismiss(); onOpenTickets() },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) { Text(stringResource(R.string.purchase_view_tickets)) }
        OutlinedButton(onClick = viewModel::dismiss, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 14.dp)) {
            Text(stringResource(R.string.purchase_close))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableOption(
    title: String,
    subtitle: String,
    trailing: String?,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.let { Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) }
        }
    }
}

private fun truncateCode(code: String): String =
    if (code.length > 12) "${code.take(8)}…${code.takeLast(4)}" else code

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
