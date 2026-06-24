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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.tessera.android.data.dto.PlayerDto
import com.tessera.android.data.dto.PlayerInput
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.screens.components.PLAYER_FEET
import com.tessera.android.screens.components.PLAYER_POSITIONS
import com.tessera.android.screens.components.PLAYER_STATUSES
import com.tessera.android.screens.components.prettyCategory
import com.tessera.android.screens.components.prettyFoot
import com.tessera.android.screens.components.prettyPlayerStatus
import com.tessera.android.screens.components.prettyPosition
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.TeamDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    teamId: Long,
    viewModel: TeamDetailViewModel = viewModel(),
) {
    LaunchedEffect(teamId) { viewModel.load(teamId) }
    var editing by remember { mutableStateOf<PlayerDto?>(null) }
    var creating by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<PlayerDto?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            viewModel.team?.let {
                Column(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
                    Text(prettyCategory(it.category), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.team_players_subtitle), style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
                }
            }
            when {
                viewModel.loading -> Centered { CircularProgressIndicator() }
                viewModel.error -> Centered { Text(stringResource(R.string.team_error), color = MaterialTheme.colorScheme.error) }
                viewModel.players.isEmpty() -> Centered { Text(stringResource(R.string.team_no_players), color = GlassInkMuted) }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.players) { PlayerRow(it, onEdit = { editing = it }, onDelete = { toDelete = it }) }
                }
            }
        }
        FloatingActionButton(onClick = { creating = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.player_add))
        }
    }

    if (creating) {
        PlayerSheet(
            initial = null,
            onSave = { viewModel.createPlayer(it); creating = false },
            onDismiss = { creating = false },
        )
    }
    editing?.let { player ->
        PlayerSheet(
            initial = player,
            onSave = { viewModel.updatePlayer(player.id, it); editing = null },
            onDismiss = { editing = null },
        )
    }
    toDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.player_delete_title)) },
            text = { Text(stringResource(R.string.player_delete_msg, "${player.firstName} ${player.lastName}")) },
            confirmButton = { TextButton(onClick = { viewModel.deletePlayer(player.id); toDelete = null }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSheet(initial: PlayerDto?, onSave: (PlayerInput) -> Unit, onDismiss: () -> Unit) {
    var firstName by remember { mutableStateOf(initial?.firstName ?: "") }
    var lastName by remember { mutableStateOf(initial?.lastName ?: "") }
    var position by remember { mutableStateOf(initial?.position ?: "MF") }
    var number by remember { mutableStateOf(initial?.shirtNumber?.toString() ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: "ACTIVE") }
    var birthdate by remember { mutableStateOf(initial?.birthdate ?: "") }
    var nationality by remember { mutableStateOf(initial?.nationality ?: "") }
    var foot by remember { mutableStateOf(initial?.dominantFoot ?: "") }
    var height by remember { mutableStateOf(initial?.height?.toString() ?: "") }
    var weight by remember { mutableStateOf(initial?.weight?.toString() ?: "") }
    var photoUrl by remember { mutableStateOf(initial?.photoUrl ?: "") }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    FormDialog(
        title = stringResource(if (initial == null) R.string.player_add_title else R.string.player_edit_title),
        confirmLabel = stringResource(if (initial == null) R.string.player_save else R.string.common_save),
        confirmEnabled = firstName.isNotBlank() && lastName.isNotBlank(),
        onConfirm = {
            val input = PlayerInput(
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                position = position,
                status = status,
                shirtNumber = number.toIntOrNull(),
                birthdate = birthdate.trim(),
                nationality = nationality.trim(),
                dominantFoot = foot,
                height = height.toIntOrNull(),
                weight = weight.toIntOrNull(),
                photoUrl = photoUrl.trim(),
            )
            val err = validatePlayer(input)
            if (err != null) errorRes = err else onSave(input)
        },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.player_form_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        TextField(firstName, { firstName = it }, R.string.player_first)
        TextField(lastName, { lastName = it }, R.string.player_last)

        ChipsLabel(R.string.player_position)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PLAYER_POSITIONS.forEach { p ->
                FilterChip(selected = position == p, onClick = { position = p }, label = { Text(prettyPosition(p)) })
            }
        }

        NumberField(number, { number = it }, R.string.player_number)

        ChipsLabel(R.string.player_status)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PLAYER_STATUSES.forEach { s ->
                FilterChip(selected = status == s, onClick = { status = s }, label = { Text(prettyPlayerStatus(s)) })
            }
        }

        TextField(birthdate, { birthdate = it }, R.string.player_birthdate, KeyboardType.Number, R.string.player_birthdate_hint)
        TextField(nationality, { nationality = it.uppercase() }, R.string.player_nationality, hintRes = R.string.player_nationality_hint)

        ChipsLabel(R.string.player_foot)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = foot.isBlank(), onClick = { foot = "" }, label = { Text(stringResource(R.string.player_foot_none)) })
            PLAYER_FEET.forEach { f ->
                FilterChip(selected = foot == f, onClick = { foot = f }, label = { Text(prettyFoot(f)) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) { NumberField(height, { height = it }, R.string.player_height) }
            Box(Modifier.weight(1f)) { NumberField(weight, { weight = it }, R.string.player_weight) }
        }
        TextField(photoUrl, { photoUrl = it }, R.string.player_photo, hintRes = R.string.player_photo_hint)

        errorRes?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    }
}

private fun validatePlayer(i: PlayerInput): Int? {
    i.shirtNumber?.let { if (it !in 1..99) return R.string.player_err_number }
    i.nationality?.takeIf { it.isNotBlank() }?.let { if (it.length != 3) return R.string.player_err_nationality }
    i.height?.let { if (it !in 100..250) return R.string.player_err_height }
    i.weight?.let { if (it !in 30..200) return R.string.player_err_weight }
    return null
}

@Composable
private fun PlayerRow(player: PlayerDto, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(player.shirtNumber?.toString() ?: "—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(36.dp))
            Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
                Text("${player.firstName} ${player.lastName}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("${prettyPosition(player.position)} · ${prettyPlayerStatus(player.status)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ChipsLabel(labelRes: Int) {
    Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TextField(value: String, onChange: (String) -> Unit, labelRes: Int, keyboard: KeyboardType = KeyboardType.Text, hintRes: Int? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        placeholder = hintRes?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
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

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
