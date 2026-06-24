package com.tessera.android.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.tessera.android.R
import com.tessera.android.ui.theme.GlassInk
import com.tessera.android.ui.theme.GlassInkMuted

/** Cores para campos de pesquisa que assentam sobre o fundo claro. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun searchFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = GlassInk,
    unfocusedTextColor = GlassInk,
    cursorColor = GlassInk,
    focusedBorderColor = GlassInk,
    unfocusedBorderColor = GlassInkMuted,
    focusedLabelColor = GlassInk,
    unfocusedLabelColor = GlassInkMuted,
    focusedLeadingIconColor = GlassInkMuted,
    unfocusedLeadingIconColor = GlassInkMuted,
    focusedPlaceholderColor = GlassInkMuted,
    unfocusedPlaceholderColor = GlassInkMuted,
)

/** Popup de formulário (substitui os bottom sheets de add/edit). */
@Composable
fun FormDialog(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                content()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                    Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
                }
            }
        }
    }
}
