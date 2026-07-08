package com.tessera.android.screens

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tessera.android.R
import com.tessera.android.shared.ServerConfig
import com.tessera.android.ui.theme.GlassInkMuted
import kotlinx.coroutines.delay

private const val LOGO_FADE_MS = 600
private const val TEXT_DELAY_MS = 350
private const val HOLD_MS = 900L
private const val FADE_OUT_MS = 300

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    var logoVisible by remember { mutableStateOf(false) }
    var textVisible by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (leaving) 1.04f else if (logoVisible) 1f else 0.7f,
        animationSpec = tween(durationMillis = LOGO_FADE_MS, easing = EaseOutCubic),
        label = "logo-scale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (leaving) 0f else if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (leaving) FADE_OUT_MS else LOGO_FADE_MS),
        label = "logo-alpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (leaving) 0f else if (textVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (leaving) FADE_OUT_MS else LOGO_FADE_MS),
        label = "text-alpha",
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        delay(TEXT_DELAY_MS.toLong())
        textVisible = true
        delay(LOGO_FADE_MS.toLong() + HOLD_MS)
        leaving = true
        delay(FADE_OUT_MS.toLong())
        onContinue()
    }

    // Fire-and-forget: this screen navigates itself away (and cancels its own
    // LaunchedEffects) after a fixed delay that can be shorter than the probe
    // takes, so the resolve itself runs in ServerConfig's own scope instead of
    // this one — otherwise it can get cancelled mid-probe before ever
    // resolving, permanently stranding mode at UNKNOWN for the session.
    LaunchedEffect(Unit) {
        ServerConfig.resolveModeInBackground()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_tessera_logo),
            contentDescription = stringResource(R.string.welcome_logo_desc),
            modifier = Modifier
                .size(132.dp)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                },
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.menu_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = GlassInkMuted,
            modifier = Modifier.graphicsLayer { alpha = textAlpha },
        )
    }
}
