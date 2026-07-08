package com.tessera.android.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.screens.components.ClubCrest
import com.tessera.android.screens.components.QrView
import com.tessera.android.screens.components.StatusBadge
import com.tessera.android.screens.components.formatEur
import com.tessera.android.screens.components.formatKickoffFull
import com.tessera.android.screens.components.formatShortDateTime
import com.tessera.android.screens.components.RefreshOnResume
import com.tessera.android.ui.theme.GlassInk
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.MyTicketsState
import com.tessera.android.viewmodels.MyTicketsViewModel
import com.tessera.android.viewmodels.TicketView

@Composable
fun MyTicketsScreen(
    onOpenEvent: (Long) -> Unit = {},
    viewModel: MyTicketsViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    RefreshOnResume { viewModel.load() }

    fun addToWallet(v: TicketView) {
        viewModel.addToWallet(v) { url ->
            if (url != null) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    when (val s = viewModel.state) {
        MyTicketsState.Loading -> Centered {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.tickets_loading), color = GlassInkMuted)
        }
        MyTicketsState.Error -> Centered {
            Text(stringResource(R.string.tickets_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
        }
        is MyTicketsState.Success -> TicketsList(s, onOpenEvent, viewModel.walletLoadingId, ::addToWallet)
    }
}

@Composable
private fun TicketsList(
    s: MyTicketsState.Success,
    onOpenEvent: (Long) -> Unit,
    walletLoadingId: Long?,
    onAddToWallet: (TicketView) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                stringResource(R.string.tickets_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = GlassInkMuted,
            )
        }

        item { SectionTitle(stringResource(R.string.tickets_section_ready)) }
        if (s.paid.isEmpty()) {
            item { EmptyHint(stringResource(R.string.tickets_paid_empty)) }
        } else {
            items(s.paid, key = { it.ticket.id }) {
                PaidTicketCard(it, isWalletLoading = walletLoadingId == it.ticket.id, onAddToWallet = onAddToWallet)
            }
        }

        if (s.pending.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.tickets_section_pending), stringResource(R.string.tickets_pending_subtitle)) }
            items(s.pending, key = { it.ticket.id }) { PendingTicketCard(it, onOpenEvent) }
        }

        item { SectionTitle(stringResource(R.string.tickets_section_history)) }
        if (s.past.isEmpty()) {
            item { EmptyHint(stringResource(R.string.tickets_history_empty)) }
        } else {
            item { HistoryCard(s.past) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = GlassInk)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
        }
    }
}

@Composable
private fun PaidTicketCard(v: TicketView, isWalletLoading: Boolean, onAddToWallet: (TicketView) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusBadge("PAID")
                Text("#${v.ticket.id}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(matchupText(v), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(formatKickoffFull(v.entry?.kickoffAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                QrView(content = v.ticket.code, modifier = Modifier.size(180.dp))
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.tickets_code_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(truncateCode(v.ticket.code), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            DetailRow(stringResource(R.string.tickets_local), v.entry?.venueName ?: "—")
            DetailRow(stringResource(R.string.tickets_type), tierText(v))
            DetailRow(stringResource(R.string.tickets_price), formatEur(v.ticket.price))

            formatShortDateTime(v.ticket.paymentDate)?.let {
                Text(stringResource(R.string.tickets_paid_at, it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = { onAddToWallet(v) },
                enabled = !isWalletLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (isWalletLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.tickets_add_to_wallet), color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PendingTicketCard(v: TicketView, onOpenEvent: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge("PENDING")
                Text(stringResource(R.string.tickets_pending_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(matchupText(v), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${formatKickoffFull(v.entry?.kickoffAt)} · ${tierText(v)} · ${formatEur(v.ticket.price)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.tickets_pending_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Button(onClick = { onOpenEvent(v.ticket.eventId) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tickets_finish_payment))
            }
        }
    }
}

@Composable
private fun HistoryCard(past: List<TicketView>) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            past.forEachIndexed { i, v ->
                PastTicketRow(v)
                if (i < past.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun PastTicketRow(v: TicketView) {
    Row(
        modifier = Modifier.fillMaxWidth().alpha(0.65f).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        v.entry?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ClubCrest(it.homeInitials, it.homeTone, 28.dp)
                ClubCrest(it.awayInitials, it.awayTone, 28.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(matchupText(v), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("${formatKickoffFull(v.entry?.kickoffAt)} · ${tierText(v)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusBadge("VALIDATED")
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun matchupText(v: TicketView): String =
    v.entry?.let { "${it.homeShort} ${stringResource(R.string.match_vs)} ${it.awayShort}" }
        ?: stringResource(R.string.events_unnamed)

@Composable
private fun tierText(v: TicketView): String =
    stringResource(if (v.supporter) R.string.price_supporter else R.string.price_normal)

private fun truncateCode(code: String): String =
    if (code.length > 12) "${code.take(8)}…${code.takeLast(4)}" else code

@Composable
private fun EmptyHint(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(24.dp),
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
