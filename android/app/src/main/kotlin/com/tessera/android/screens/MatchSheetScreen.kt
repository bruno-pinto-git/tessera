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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.PlayerDto
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.screens.components.LINEUP_ROLES
import com.tessera.android.screens.components.OCCURRENCE_TYPES
import com.tessera.android.screens.components.StatusBadge
import com.tessera.android.screens.components.occurrenceIcon
import com.tessera.android.screens.components.prettyLineupRole
import com.tessera.android.screens.components.prettyOccurrenceType
import com.tessera.android.viewmodels.MatchSheetViewModel

@Composable
fun MatchSheetScreen(
    matchId: Long,
    isAdmin: Boolean = false,
    viewModel: MatchSheetViewModel = viewModel(),
) {
    LaunchedEffect(matchId) { viewModel.load(matchId) }
    var addLineupTeam by remember { mutableStateOf<Pair<Long, List<PlayerDto>>?>(null) }
    var showAddOccurrence by remember { mutableStateOf(false) }

    val match = viewModel.match
    when {
        viewModel.loading -> Centered { CircularProgressIndicator() }
        match == null -> Centered {
            Text(stringResource(R.string.sheet_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { viewModel.load(matchId) }) { Text(stringResource(R.string.common_retry)) }
        }
        else -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HeaderCard(viewModel)
            LockSection(viewModel, isAdmin)
            TeamLineupCard(viewModel, match.homeTeamId, viewModel.homeName, viewModel.homeRoster) { t, r -> addLineupTeam = t to r }
            TeamLineupCard(viewModel, match.awayTeamId, viewModel.awayName, viewModel.awayRoster) { t, r -> addLineupTeam = t to r }
            OccurrencesCard(viewModel) { showAddOccurrence = true }
        }
    }

    addLineupTeam?.let { (teamId, roster) ->
        AddLineupDialog(
            available = viewModel.availableFor(teamId, roster),
            onConfirm = { playerId, role, shirt -> viewModel.addLineup(playerId, role, shirt); addLineupTeam = null },
            onDismiss = { addLineupTeam = null },
        )
    }
    if (showAddOccurrence && match != null) {
        AddOccurrenceDialog(
            vm = viewModel,
            match = match,
            onConfirm = { minute, type, playerId, replaced -> viewModel.addOccurrence(minute, type, playerId, replaced); showAddOccurrence = false },
            onDismiss = { showAddOccurrence = false },
        )
    }
}

@Composable
private fun HeaderCard(vm: MatchSheetViewModel) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${vm.homeName} vs ${vm.awayName}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.match?.status?.let { StatusBadge(it) }
                if (vm.locked) {
                    Text(stringResource(R.string.sheet_locked_badge), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LockSection(vm: MatchSheetViewModel, isAdmin: Boolean) {
    if (!vm.locked) {
        Button(onClick = vm::lock, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sheet_lock))
        }
    } else if (isAdmin) {
        OutlinedButton(onClick = vm::unlock, enabled = !vm.busy, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sheet_unlock))
        }
    }
}

@Composable
private fun TeamLineupCard(vm: MatchSheetViewModel, teamId: Long, teamName: String, roster: List<PlayerDto>, onAdd: (Long, List<PlayerDto>) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(teamName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!vm.locked) {
                    TextButton(onClick = { onAdd(teamId, roster) }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.sheet_call_up))
                    }
                }
            }
            RoleBlock(stringResource(R.string.sheet_starters), vm, teamId, "STARTER")
            RoleBlock(stringResource(R.string.sheet_subs), vm, teamId, "SUBSTITUTE")
        }
    }
}

@Composable
private fun RoleBlock(label: String, vm: MatchSheetViewModel, teamId: Long, role: String) {
    val entries = vm.lineupFor(teamId, role)
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (entries.isEmpty()) {
        Text(stringResource(R.string.sheet_no_lineup), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        entries.forEach { e ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${e.shirtNumber?.let { "$it. " } ?: ""}${vm.playerName(e.playerId)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!vm.locked) {
                    IconButton(onClick = { vm.removeLineup(e.playerId) }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.error, modifier = Modifier.width(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OccurrencesCard(vm: MatchSheetViewModel, onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.sheet_occurrences), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                if (!vm.locked) {
                    TextButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.sheet_add_occurrence))
                    }
                }
            }
            val occ = vm.sheet?.occurrences?.sortedBy { it.minute } ?: emptyList()
            if (occ.isEmpty()) {
                Text(stringResource(R.string.sheet_no_occurrences), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                occ.forEachIndexed { i, o ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${o.minute}'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp))
                        Text(occurrenceIcon(o.type))
                        Column(Modifier.weight(1f)) {
                            Text("${prettyOccurrenceType(o.type)} — ${vm.playerName(o.playerId)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            o.replacedPlayerId?.let {
                                Text(stringResource(R.string.sheet_entered_for, vm.playerName(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (!vm.locked) {
                            IconButton(onClick = { vm.removeOccurrence(o.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.error, modifier = Modifier.width(20.dp))
                            }
                        }
                    }
                    if (i < occ.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLineupDialog(available: List<PlayerDto>, onConfirm: (Long, String, Int?) -> Unit, onDismiss: () -> Unit) {
    var playerId by remember { mutableStateOf<Long?>(null) }
    var role by remember { mutableStateOf("STARTER") }
    var shirt by remember { mutableStateOf("") }

    FormDialog(
        title = stringResource(R.string.sheet_call_up),
        confirmLabel = stringResource(R.string.member_add_existing),
        confirmEnabled = playerId != null,
        onConfirm = { playerId?.let { onConfirm(it, role, shirt.toIntOrNull()) } },
        onDismiss = onDismiss,
    ) {
        if (available.isEmpty()) {
            Text(stringResource(R.string.sheet_no_available_players), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownField(
            label = stringResource(R.string.sheet_player),
            selectedText = available.firstOrNull { it.id == playerId }?.let { playerLabel(it) },
            items = available,
            labelOf = { playerLabel(it) },
        ) { playerId = it.id }
        ChipsRow(stringResource(R.string.sheet_role), LINEUP_ROLES, role, { prettyLineupRole(it) }) { role = it }
        NumberField(shirt, { shirt = it }, R.string.sheet_shirt)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddOccurrenceDialog(vm: MatchSheetViewModel, match: MatchDto, onConfirm: (Int, String, Long, Long?) -> Unit, onDismiss: () -> Unit) {
    var minute by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("GOAL") }
    var teamId by remember { mutableStateOf(match.homeTeamId) }
    var playerId by remember { mutableStateOf<Long?>(null) }
    var replacedId by remember { mutableStateOf<Long?>(null) }

    val called = vm.calledUp(teamId)

    FormDialog(
        title = stringResource(R.string.sheet_add_occurrence),
        confirmLabel = stringResource(R.string.sheet_register),
        confirmEnabled = playerId != null && minute.toIntOrNull() != null,
        onConfirm = {
            val m = minute.toIntOrNull()
            val pid = playerId
            if (m != null && pid != null) {
                onConfirm(m, type, pid, if (type == "SUBSTITUTION") replacedId else null)
            }
        },
        onDismiss = onDismiss,
    ) {
        NumberField(minute, { minute = it }, R.string.sheet_minute)
        DropdownField(stringResource(R.string.sheet_type), prettyOccurrenceType(type), OCCURRENCE_TYPES, { prettyOccurrenceType(it) }) { type = it }
        ChipsRow(stringResource(R.string.sheet_team), listOf(match.homeTeamId, match.awayTeamId), teamId, { if (it == match.homeTeamId) vm.homeName else vm.awayName }) {
            teamId = it; playerId = null; replacedId = null
        }
        DropdownField(
            label = stringResource(R.string.sheet_player),
            selectedText = playerId?.let { vm.playerName(it) },
            items = called.map { it.playerId },
            labelOf = { vm.playerName(it) },
        ) { playerId = it }
        if (type == "SUBSTITUTION") {
            DropdownField(
                label = stringResource(R.string.sheet_replaced),
                selectedText = replacedId?.let { vm.playerName(it) },
                items = called.map { it.playerId }.filter { it != playerId },
                labelOf = { vm.playerName(it) },
            ) { replacedId = it }
        }
    }
}

@Composable
private fun <T> DropdownField(label: String, selectedText: String?, items: List<T>, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedText ?: "—", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                items.forEach { item ->
                    DropdownMenuItem(text = { Text(labelOf(item)) }, onClick = { onSelect(item); open = false })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipsRow(label: String, items: List<T>, selected: T, labelOf: (T) -> String, onSelect: (T) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            FilterChip(selected = selected == item, onClick = { onSelect(item) }, label = { Text(labelOf(item)) })
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, labelRes: Int) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter { c -> c.isDigit() }) },
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun playerLabel(p: PlayerDto): String =
    "${p.shirtNumber?.let { "$it. " } ?: ""}${p.firstName} ${p.lastName}"

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
