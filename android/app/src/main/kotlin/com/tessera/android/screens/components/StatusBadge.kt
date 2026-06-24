package com.tessera.android.screens.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tessera.android.ui.theme.StatusUnknown

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            text = prettyStatusLabel(status),
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

fun prettyStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Pendente"
    "PAID" -> "Pago"
    "VALIDATED" -> "Validado"
    "CANCELLED" -> "Cancelado"
    "SCHEDULED" -> "Agendado"
    "LIVE" -> "Ao vivo"
    "FINISHED" -> "Terminado"
    "POSTPONED" -> "Adiado"
    "ABANDONED" -> "Abandonado"
    "SALES_CLOSED" -> "Encerrada"
    "PUBLISHED" -> "Aberta"
    else -> status
}

@Composable
private fun statusColor(status: String): Color = when (status) {
    "PAID", "LIVE", "PUBLISHED" -> MaterialTheme.colorScheme.primary
    "PENDING", "SCHEDULED" -> StatusUnknown
    "VALIDATED", "FINISHED" -> MaterialTheme.colorScheme.secondary
    "CANCELLED", "ABANDONED", "SALES_CLOSED" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
