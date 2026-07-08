package com.tessera.mockmbway.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.mockmbway.R
import com.tessera.mockmbway.shared.PendingPayment
import com.tessera.mockmbway.shared.RelayConfig
import com.tessera.mockmbway.shared.Resolution
import com.tessera.mockmbway.shared.ResolvedPayment
import com.tessera.mockmbway.ui.theme.Neutral500
import com.tessera.mockmbway.ui.theme.StatusInvalid
import com.tessera.mockmbway.ui.theme.TesseraForest
import com.tessera.mockmbway.viewmodels.PaymentsViewModel

@Composable
fun PaymentsScreen(onOpenSettings: () -> Unit = {}, viewModel: PaymentsViewModel = viewModel()) {
    val pending = viewModel.pending
    val history = viewModel.history

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Header(onOpenSettings = onOpenSettings)
        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (pending.isEmpty()) {
                item { EmptyState() }
            } else {
                item { SectionLabel(stringResource(R.string.section_pending)) }
                items(pending, key = { "p-${it.transactionId}" }) { payment ->
                    PaymentCard(
                        payment = payment,
                        onAccept = { viewModel.accept(payment) },
                        onDecline = { viewModel.decline(payment) },
                    )
                }
            }
            if (history.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item { SectionLabel(stringResource(R.string.section_history)) }
                items(history, key = { "h-${it.transactionId}-${it.resolvedAt}" }) { resolved ->
                    HistoryRow(resolved = resolved)
                }
            }
        }
    }
}

@Composable
private fun Header(onOpenSettings: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.header_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.header_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(12.dp))
            ConnectionStatus()
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionStatus() {
    val connected = RelayConfig.mode != RelayConfig.Mode.UNKNOWN && RelayConfig.lastPollOk != false
    val label = when {
        RelayConfig.mode == RelayConfig.Mode.UNKNOWN -> stringResource(R.string.status_not_connected)
        RelayConfig.lastPollOk == false -> stringResource(R.string.status_lost, RelayConfig.host)
        else -> stringResource(R.string.status_connected, RelayConfig.host)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (connected) TesseraForest else StatusInvalid,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PaymentCard(
    payment: PendingPayment,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp),
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Text(
            text = payment.description ?: payment.merchantTransactionId,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(12.dp))

        LabelRow(label = stringResource(R.string.amount_label), value = formatAmount(payment.amount, payment.currency))
        payment.customerPhone?.let {
            Spacer(Modifier.height(4.dp))
            LabelRow(label = stringResource(R.string.phone_label), value = it)
        }
        Spacer(Modifier.height(4.dp))
        LabelRow(
            label = stringResource(R.string.reference_label),
            value = payment.merchantTransactionId,
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusInvalid),
            ) {
                Text(stringResource(R.string.action_decline), style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 12.dp),
            ) {
                Text(stringResource(R.string.action_accept), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HistoryRow(resolved: ResolvedPayment) {
    val (label, color) = when (resolved.resolution) {
        Resolution.ACCEPTED -> stringResource(R.string.resolution_accepted) to TesseraForest
        Resolution.DECLINED -> stringResource(R.string.resolution_declined) to StatusInvalid
        Resolution.EXPIRED -> stringResource(R.string.resolution_expired) to Neutral500
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resolved.description ?: resolved.merchantTransactionId,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = formatAmount(resolved.amount, resolved.currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}

@Composable
private fun LabelRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private fun formatAmount(value: Double, currency: String): String =
    "%.2f %s".format(value, currency)
