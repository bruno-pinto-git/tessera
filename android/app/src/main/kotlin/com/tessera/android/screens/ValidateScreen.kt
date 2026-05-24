package com.tessera.android.screens

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.shared.ScanState
import com.tessera.android.ui.theme.StatusInvalid
import com.tessera.android.ui.theme.StatusInvalidSoft
import com.tessera.android.ui.theme.StatusNetwork
import com.tessera.android.ui.theme.StatusNetworkSoft
import com.tessera.android.ui.theme.StatusUnknown
import com.tessera.android.ui.theme.StatusUnknownSoft
import com.tessera.android.ui.theme.StatusUsed
import com.tessera.android.ui.theme.StatusUsedSoft
import com.tessera.android.ui.theme.StatusValid
import com.tessera.android.ui.theme.TesseraForest
import com.tessera.android.ui.theme.TesseraForestSoft
import com.tessera.android.viewmodels.InvalidReason
import com.tessera.android.viewmodels.ValidateViewModel
import com.tessera.android.viewmodels.ValidationState

@Composable
fun ValidateScreen(viewModel: ValidateViewModel = viewModel()) {
    val scanActive by ScanState.scanActive
    val state = viewModel.validationState

    LaunchedEffect(scanActive) {
        if (scanActive) viewModel.startScan() else viewModel.stopScan()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Header()
        Spacer(modifier = Modifier.height(48.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val display = resolveDisplay(state = state, scanActive = scanActive)
            StateView(
                icon = display.icon,
                iconDescription = display.iconDescription,
                title = display.title,
                subtitle = display.subtitle,
                accent = display.accent,
                halo = display.halo,
            )
        }
    }
}

@Composable
private fun Header() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.validate_eyebrow).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.validate_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

private data class Display(
    val key: String,
    val icon: ImageVector,
    val iconDescription: String,
    val title: String,
    val subtitle: String? = null,
    val accent: Color,
    val halo: Color,
)

@Composable
private fun resolveDisplay(state: ValidationState, scanActive: Boolean): Display = when (state) {
    ValidationState.Idle -> if (scanActive) scanningDisplay() else idleDisplay()
    ValidationState.Valid -> validDisplay()
    is ValidationState.Invalid -> invalidDisplay(state.reason)
}

@Composable
private fun idleDisplay() = Display(
    key = "idle",
    icon = Icons.Filled.QrCodeScanner,
    iconDescription = stringResource(R.string.validate_idle_icon_desc),
    title = stringResource(R.string.validate_idle_title),
    subtitle = stringResource(R.string.validate_idle_subtitle),
    accent = MaterialTheme.colorScheme.onSurfaceVariant,
    halo = MaterialTheme.colorScheme.surfaceVariant,
)

@Composable
private fun scanningDisplay() = Display(
    key = "scanning",
    icon = Icons.Filled.QrCodeScanner,
    iconDescription = stringResource(R.string.validate_idle_icon_desc),
    title = stringResource(R.string.validate_scanning_title),
    subtitle = null,
    accent = TesseraForest,
    halo = TesseraForestSoft,
)

@Composable
private fun validDisplay() = Display(
    key = "valid",
    icon = Icons.Filled.CheckCircle,
    iconDescription = stringResource(R.string.validate_valid_icon_desc),
    title = stringResource(R.string.validate_valid_title),
    subtitle = null,
    accent = StatusValid,
    halo = TesseraForestSoft,
)

@Composable
private fun invalidDisplay(reason: InvalidReason): Display {
    val titleRes = when (reason) {
        InvalidReason.ALREADY_USED -> R.string.validate_invalid_used_title
        InvalidReason.NOT_FOUND -> R.string.validate_invalid_notfound_title
        InvalidReason.BAD_CODE -> R.string.validate_invalid_badcode_title
        InvalidReason.AUTH -> R.string.validate_invalid_auth_title
        InvalidReason.NETWORK -> R.string.validate_invalid_network_title
        InvalidReason.UNKNOWN -> R.string.validate_invalid_unknown_title
    }

    val icon: ImageVector
    val accent: Color
    val halo: Color
    when (reason) {
        InvalidReason.NOT_FOUND -> {
            icon = Icons.Filled.HelpOutline
            accent = StatusUnknown
            halo = StatusUnknownSoft
        }
        InvalidReason.ALREADY_USED -> {
            icon = Icons.Filled.History
            accent = StatusUsed
            halo = StatusUsedSoft
        }
        InvalidReason.NETWORK -> {
            icon = Icons.Filled.CloudOff
            accent = StatusNetwork
            halo = StatusNetworkSoft
        }
        else -> {
            icon = Icons.Filled.Cancel
            accent = StatusInvalid
            halo = StatusInvalidSoft
        }
    }

    return Display(
        key = "invalid:${reason.name}",
        icon = icon,
        iconDescription = stringResource(R.string.validate_invalid_icon_desc),
        title = stringResource(titleRes),
        subtitle = null,
        accent = accent,
        halo = halo,
    )
}

@Composable
private fun StateView(
    icon: ImageVector,
    iconDescription: String,
    title: String,
    subtitle: String? = null,
    accent: Color,
    halo: Color,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo-pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo-scale",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(176.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(halo)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = CircleShape),
            )
            Icon(
                imageVector = icon,
                contentDescription = iconDescription,
                modifier = Modifier.size(96.dp),
                tint = accent,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = accent,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
