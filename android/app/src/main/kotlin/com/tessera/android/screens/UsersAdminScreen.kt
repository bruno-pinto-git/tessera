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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.UserDto
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.screens.components.RefreshOnResume
import com.tessera.android.screens.components.USER_CREATE_ROLES
import com.tessera.android.screens.components.prettyUserRole
import com.tessera.android.screens.components.searchFieldColors
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.AdminUsersState
import com.tessera.android.viewmodels.UsersAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersAdminScreen(viewModel: UsersAdminViewModel = viewModel()) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UserDto?>(null) }
    var resetting by remember { mutableStateOf<UserDto?>(null) }
    var toDelete by remember { mutableStateOf<UserDto?>(null) }
    var query by remember { mutableStateOf("") }
    RefreshOnResume { viewModel.load() }

    Box(Modifier.fillMaxSize()) {
        when (val s = viewModel.state) {
            AdminUsersState.Loading -> Centered { CircularProgressIndicator() }
            AdminUsersState.Error -> Centered {
                Text(stringResource(R.string.users_error), color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
            }
            is AdminUsersState.Success -> {
                Column(Modifier.fillMaxSize()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        placeholder = { Text(stringResource(R.string.users_search_hint)) },
                        colors = searchFieldColors(),
                    )
                    val q = query.trim().lowercase()
                    val filtered = s.users.filter { u ->
                        q.isBlank() || listOfNotNull(u.username, u.firstName, u.lastName, u.email).any { it.lowercase().contains(q) }
                    }
                    when {
                        s.users.isEmpty() -> Centered { Text(stringResource(R.string.users_empty), color = GlassInkMuted) }
                        filtered.isEmpty() -> Centered { Text(stringResource(R.string.users_search_empty), color = GlassInkMuted) }
                        else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(filtered) { user ->
                                UserRow(
                                    user = user,
                                    onToggle = { viewModel.setEnabled(user.id, !user.enabled) },
                                    onForce = { resetting = user },
                                    onEdit = { editing = user },
                                    onDelete = { toDelete = user },
                                )
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.user_add))
        }
    }

    if (showAdd) {
        AddUserSheet(
            onAdd = { u, e, f, l, p, role -> viewModel.createUser(u, e, f, l, p, role); showAdd = false },
            onDismiss = { showAdd = false },
        )
    }
    editing?.let { user ->
        EditUserDialog(
            user = user,
            onSave = { email, firstName, lastName, role -> viewModel.updateUser(user.id, email, firstName, lastName, role); editing = null },
            onDismiss = { editing = null },
        )
    }
    resetting?.let { user ->
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text(stringResource(R.string.user_force_password)) },
            text = { Text(stringResource(R.string.user_force_password_msg, userName(user))) },
            confirmButton = { TextButton(onClick = { viewModel.forcePasswordReset(user.id); resetting = null }) { Text(stringResource(R.string.user_force_confirm)) } },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
    toDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.user_delete_title)) },
            text = { Text(userName(user)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteUser(user.id); toDelete = null }) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun UserRow(user: UserDto, onToggle: () -> Unit, onForce: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val role = user.roles.firstOrNull { it != "fan" } ?: user.roles.firstOrNull()
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(userName(user), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                StatusPill(user.enabled)
            }
            Text(
                buildString {
                    append(user.username ?: "")
                    if (role != null) append(" · ${prettyUserRole(role)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    if (user.enabled) {
                        Icon(Icons.Filled.Block, contentDescription = stringResource(R.string.user_deactivate), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.user_activate), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onForce) { Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.user_force_password), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun StatusPill(enabled: Boolean) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(50)) {
        Text(
            stringResource(if (enabled) R.string.user_status_active else R.string.user_status_disabled),
            color = color,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddUserSheet(onAdd: (String, String, String, String, String, String) -> Unit, onDismiss: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("staff") }

    FormDialog(
        title = stringResource(R.string.user_add_title),
        confirmLabel = stringResource(R.string.user_save),
        confirmEnabled = username.length >= 3 && firstName.isNotBlank() && lastName.isNotBlank() && password.length >= 6,
        onConfirm = { onAdd(username.trim(), email.trim(), firstName.trim(), lastName.trim(), password, role) },
        onDismiss = onDismiss,
    ) {
        OutlinedTextField(username, { username = it }, label = { Text(stringResource(R.string.member_username)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.member_email)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(firstName, { firstName = it }, label = { Text(stringResource(R.string.player_first)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(lastName, { lastName = it }, label = { Text(stringResource(R.string.player_last)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.member_password)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.member_role), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            USER_CREATE_ROLES.forEach { r -> FilterChip(selected = role == r, onClick = { role = r }, label = { Text(prettyUserRole(r)) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditUserDialog(user: UserDto, onSave: (String, String, String, String?) -> Unit, onDismiss: () -> Unit) {
    val isAdmin = "platform-admin" in user.roles
    var email by remember { mutableStateOf(user.email ?: "") }
    var firstName by remember { mutableStateOf(user.firstName ?: "") }
    var lastName by remember { mutableStateOf(user.lastName ?: "") }
    var role by remember { mutableStateOf(user.roles.firstOrNull { it in USER_CREATE_ROLES } ?: "club-manager") }

    FormDialog(
        title = stringResource(R.string.user_edit_title),
        confirmLabel = stringResource(R.string.common_save),
        confirmEnabled = firstName.isNotBlank() && lastName.isNotBlank(),
        onConfirm = { onSave(email.trim(), firstName.trim(), lastName.trim(), if (isAdmin) null else role) },
        onDismiss = onDismiss,
    ) {
        Text("@${user.username ?: "—"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(email, { email = it }, label = { Text(stringResource(R.string.member_email)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(firstName, { firstName = it }, label = { Text(stringResource(R.string.player_first)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(lastName, { lastName = it }, label = { Text(stringResource(R.string.player_last)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (!isAdmin) {
            Text(stringResource(R.string.member_role), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                USER_CREATE_ROLES.forEach { r -> FilterChip(selected = role == r, onClick = { role = r }, label = { Text(prettyUserRole(r)) }) }
            }
        }
    }
}

private fun userName(user: UserDto): String =
    listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { user.username ?: user.id }

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
