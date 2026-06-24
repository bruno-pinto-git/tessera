package com.tessera.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.VenueDto
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.AdminVenuesState
import com.tessera.android.viewmodels.VenuesAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenuesAdminScreen(viewModel: VenuesAdminViewModel = viewModel()) {
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<VenueDto?>(null) }

    Box(Modifier.fillMaxSize()) {
        when (val s = viewModel.state) {
            AdminVenuesState.Loading -> Centered { CircularProgressIndicator() }
            AdminVenuesState.Error -> Centered {
                Text(stringResource(R.string.venues_error), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
            }
            is AdminVenuesState.Success -> {
                if (s.venues.isEmpty()) {
                    Centered { Text(stringResource(R.string.venues_empty), color = GlassInkMuted) }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(s.venues) { venue ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                                        Text(venue.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Text(stringResource(R.string.venue_capacity_value, venue.capacity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { toDelete = venue }) { Icon(Icons.Filled.Delete, stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.venue_add))
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        FormDialog(
            title = stringResource(R.string.venue_add_title),
            confirmLabel = stringResource(R.string.common_save),
            confirmEnabled = name.isNotBlank() && capacity.toIntOrNull() != null,
            onConfirm = { viewModel.createVenue(name.trim(), capacity.toIntOrNull() ?: 0, address.trim()); showAdd = false },
            onDismiss = { showAdd = false },
        ) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.venue_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(capacity, { capacity = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.venue_capacity)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it }, label = { Text(stringResource(R.string.venue_address)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }

    toDelete?.let { venue ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.venue_delete_title)) },
            text = { Text(venue.name) },
            confirmButton = { TextButton(onClick = { viewModel.deleteVenue(venue.id); toDelete = null }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
