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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.screens.components.RefreshOnResume
import com.tessera.android.screens.components.searchFieldColors
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.AdminClubsState
import com.tessera.android.viewmodels.ClubsAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubsAdminScreen(
    onClubClick: (Long) -> Unit,
    viewModel: ClubsAdminViewModel = viewModel(),
) {
    var showAdd by remember { mutableStateOf(false) }
    var toEdit by remember { mutableStateOf<ClubDto?>(null) }
    var toDelete by remember { mutableStateOf<ClubDto?>(null) }
    var query by remember { mutableStateOf("") }
    RefreshOnResume { viewModel.load() }

    Box(Modifier.fillMaxSize()) {
        when (val s = viewModel.state) {
            AdminClubsState.Loading -> Centered { CircularProgressIndicator() }
            AdminClubsState.Error -> Centered {
                Text(stringResource(R.string.clubs_error), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
            }
            is AdminClubsState.Success -> {
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.clubs_search_hint)) },
                        colors = searchFieldColors(),
                    )
                    val filtered = s.clubs.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    when {
                        s.clubs.isEmpty() -> Centered { Text(stringResource(R.string.clubs_empty), color = GlassInkMuted) }
                        filtered.isEmpty() -> Centered { Text(stringResource(R.string.clubs_search_empty), color = GlassInkMuted) }
                        else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered) { club ->
                                Card(onClick = { onClubClick(club.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                                            Text(club.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                            club.foundedYear?.let { Text(stringResource(R.string.clubs_founded, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        IconButton(onClick = { toEdit = club }) { Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                    IconButton(onClick = { toDelete = club }) { Icon(Icons.Filled.Delete, stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.club_add))
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        var year by remember { mutableStateOf("") }
        FormDialog(
            title = stringResource(R.string.club_add_title),
            confirmLabel = stringResource(R.string.common_save),
            confirmEnabled = name.isNotBlank(),
            onConfirm = { viewModel.createClub(name.trim(), year.toIntOrNull()); showAdd = false },
            onDismiss = { showAdd = false },
        ) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.club_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(year, { year = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.club_founded_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
    }

    toEdit?.let { club ->
        var name by remember { mutableStateOf(club.name) }
        var year by remember { mutableStateOf(club.foundedYear?.toString() ?: "") }
        var crest by remember { mutableStateOf(club.crestUrl ?: "") }
        FormDialog(
            title = stringResource(R.string.club_edit_title),
            confirmLabel = stringResource(R.string.common_save),
            confirmEnabled = name.trim().length >= 2,
            onConfirm = { viewModel.updateClub(club.id, name.trim(), year.toIntOrNull(), crest.trim()); toEdit = null },
            onDismiss = { toEdit = null },
        ) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.club_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(year, { year = it.filter { c -> c.isDigit() } }, label = { Text(stringResource(R.string.club_founded_label)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(crest, { crest = it }, label = { Text(stringResource(R.string.club_crest_url)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }

    toDelete?.let { club ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.club_delete_title)) },
            text = { Text(club.name) },
            confirmButton = { TextButton(onClick = { viewModel.deleteClub(club.id); toDelete = null }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
