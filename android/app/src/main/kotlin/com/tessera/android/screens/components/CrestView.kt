package com.tessera.android.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tessera.android.ui.theme.CrestCream
import com.tessera.android.ui.theme.CrestForest
import com.tessera.android.ui.theme.CrestNavy
import com.tessera.android.ui.theme.CrestOchre
import com.tessera.android.ui.theme.CrestOxblood
import com.tessera.android.ui.theme.CrestSlate

private val CREST_COLORS = listOf(CrestForest, CrestOxblood, CrestNavy, CrestOchre, CrestSlate, CrestCream)

@Composable
fun ClubCrest(initials: String, tone: Int, size: Dp = 56.dp) {
    val index = ((tone % CREST_COLORS.size) + CREST_COLORS.size) % CREST_COLORS.size
    val bg = CREST_COLORS[index]
    val fg = if (bg == CrestCream) Color(0xFF1A1A1A) else Color.White
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = fg, fontWeight = FontWeight.Bold, fontSize = (size.value * 0.36f).sp)
    }
}
