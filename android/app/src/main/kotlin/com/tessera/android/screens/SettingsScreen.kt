package com.tessera.android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.screens.components.searchFieldColors
import com.tessera.android.shared.ServerConfig
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_help),
            style = MaterialTheme.typography.bodyMedium,
            color = GlassInkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = viewModel.host,
            onValueChange = viewModel::onHostChange,
            label = { Text(stringResource(R.string.settings_host_label)) },
            placeholder = { Text(stringResource(R.string.settings_host_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.save() }),
            colors = searchFieldColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        when {
            viewModel.resolving -> Text(
                text = stringResource(R.string.settings_resolving),
                style = MaterialTheme.typography.bodySmall,
                color = GlassInkMuted,
            )
            viewModel.saved && viewModel.resolvedMode == ServerConfig.Mode.UNKNOWN -> Text(
                text = stringResource(R.string.settings_resolve_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            viewModel.saved -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = ServerConfig.baseUrl, style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
                Text(text = ServerConfig.issuer, style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
            }
        }
        Spacer(Modifier.height(24.dp))

        if (viewModel.saved && viewModel.resolvedMode != ServerConfig.Mode.UNKNOWN) {
            Text(
                text = stringResource(R.string.settings_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = viewModel::save,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_save),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(12.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.settings_back))
        }
    }
}
