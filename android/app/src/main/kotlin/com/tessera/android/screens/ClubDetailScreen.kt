package com.tessera.android.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.MemberDto
import com.tessera.android.data.dto.MembersDto
import com.tessera.android.data.dto.TeamDto
import com.tessera.android.data.dto.UserDto
import com.tessera.android.data.dto.VenueDto
import com.tessera.android.screens.components.CLUB_ROLES
import com.tessera.android.screens.components.FormDialog
import com.tessera.android.screens.components.StatusBadge
import com.tessera.android.screens.components.TEAM_CATEGORIES
import com.tessera.android.screens.components.formatKickoff
import com.tessera.android.screens.components.isoToLocalDate
import com.tessera.android.screens.components.isoToLocalTime
import com.tessera.android.screens.components.localDateTimeToIso
import com.tessera.android.screens.components.prettyCategory
import com.tessera.android.screens.components.prettyClubRole
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.ClubDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailScreen(
    clubId: Long,
    onTeamClick: (Long) -> Unit,
    onMatchSheetClick: (Long) -> Unit = {},
    isAdmin: Boolean = false,
    viewModel: ClubDetailViewModel = viewModel(),
) {
    LaunchedEffect(clubId) { viewModel.load(clubId) }
    var tab by remember { mutableIntStateOf(0) }
    var showEditClub by remember { mutableStateOf(false) }
    var showAddTeam by remember { mutableStateOf(false) }
    var teamToEdit by remember { mutableStateOf<TeamDto?>(null) }
    var teamToDelete by remember { mutableStateOf<TeamDto?>(null) }
    var showAddMember by remember { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Pair<MemberDto, String>?>(null) }
    var matchToEdit by remember { mutableStateOf<MatchDto?>(null) }
    var matchToDelete by remember { mutableStateOf<MatchDto?>(null) }
    var matchBoxOffice by remember { mutableStateOf<MatchDto?>(null) }

    Column(Modifier.fillMaxSize()) {
        viewModel.club?.let { club ->
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(club.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                    club.foundedYear?.let {
                        Text(stringResource(R.string.clubs_founded, it), style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
                    }
                }
                IconButton(onClick = { showEditClub = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.club_tab_teams), style = MaterialTheme.typography.labelMedium, maxLines = 1) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.club_tab_matches), style = MaterialTheme.typography.labelMedium, maxLines = 1) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.club_tab_members), style = MaterialTheme.typography.labelMedium, maxLines = 1) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text(stringResource(R.string.club_tab_sheets), style = MaterialTheme.typography.labelMedium, maxLines = 1) })
        }
        when {
            viewModel.loading -> Centered { CircularProgressIndicator() }
            viewModel.error -> Centered { Text(stringResource(R.string.club_error), color = MaterialTheme.colorScheme.error) }
            else -> when (tab) {
                0 -> TeamsTab(viewModel.teams, onTeamClick, onAdd = { showAddTeam = true }, onEdit = { teamToEdit = it }, onDelete = { teamToDelete = it })
                1 -> MatchesTab(viewModel, onBoxOffice = { matchBoxOffice = it }, onEdit = { matchToEdit = it }, onDelete = { matchToDelete = it })
                2 -> MembersTab(viewModel.members, onAdd = { showAddMember = true }, onRemove = { m, r -> memberToRemove = m to r })
                else -> SheetsTab(viewModel, onMatchSheetClick)
            }
        }
    }

    if (showEditClub) {
        viewModel.club?.let { club ->
            EditClubDialog(
                club = club,
                onSave = { name, year, crest -> viewModel.updateClub(name, year, crest); showEditClub = false },
                onDismiss = { showEditClub = false },
            )
        }
    }
    if (showAddTeam) {
        CategoryDialog(
            title = stringResource(R.string.team_add_title),
            onPick = { viewModel.createTeam(it); showAddTeam = false },
            onDismiss = { showAddTeam = false },
        )
    }
    teamToEdit?.let { team ->
        CategoryDialog(
            title = stringResource(R.string.team_edit_title),
            onPick = { viewModel.updateTeam(team.id, it); teamToEdit = null },
            onDismiss = { teamToEdit = null },
        )
    }
    teamToDelete?.let { team ->
        ConfirmDialog(
            title = stringResource(R.string.team_delete_title),
            message = prettyCategory(team.category),
            confirmLabel = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteTeam(team.id); teamToDelete = null },
            onDismiss = { teamToDelete = null },
        )
    }
    if (showAddMember) {
        AddMemberDialog(
            isAdmin = isAdmin,
            searchResults = viewModel.searchResults,
            searching = viewModel.searching,
            onSearch = viewModel::searchUsers,
            onAddExisting = { userId, role -> viewModel.addExistingMember(userId, role); showAddMember = false },
            onAddNew = { u, e, f, l, p, role -> viewModel.addMember(u, e, f, l, p, role); showAddMember = false },
            onDismiss = { viewModel.clearSearch(); showAddMember = false },
        )
    }
    memberToRemove?.let { (member, role) ->
        ConfirmDialog(
            title = stringResource(R.string.member_remove_title),
            message = memberName(member),
            confirmLabel = stringResource(R.string.common_remove),
            onConfirm = { viewModel.removeMember(member.userId, role); memberToRemove = null },
            onDismiss = { memberToRemove = null },
        )
    }
    matchToEdit?.let { match ->
        EditMatchDialog(
            match = match,
            venues = viewModel.venuesList,
            onSave = { venueId, iso, referee -> viewModel.updateMatch(match.id, venueId, iso, referee); matchToEdit = null },
            onDismiss = { matchToEdit = null },
        )
    }
    matchToDelete?.let { match ->
        ConfirmDialog(
            title = stringResource(R.string.match_delete_title),
            message = formatKickoff(match.kickoffAt),
            confirmLabel = stringResource(R.string.common_delete),
            onConfirm = { viewModel.deleteMatch(match.id); matchToDelete = null },
            onDismiss = { matchToDelete = null },
        )
    }
    matchBoxOffice?.let { match ->
        BoxOfficeDialog(
            onOpen = { name, pn, ps -> viewModel.openBoxOffice(match.id, name, pn, ps); matchBoxOffice = null },
            onDismiss = { matchBoxOffice = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TeamsTab(teams: List<TeamDto>, onTeamClick: (Long) -> Unit, onAdd: () -> Unit, onEdit: (TeamDto) -> Unit, onDelete: (TeamDto) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (teams.isEmpty()) {
                item { EmptySection(stringResource(R.string.club_no_teams)) }
            } else {
                items(teams) { team ->
                    Card(onClick = { onTeamClick(team.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(prettyCategory(team.category), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onEdit(team) }) { Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { onDelete(team) }) { Icon(Icons.Filled.Delete, stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
        Fab(onAdd, stringResource(R.string.team_add))
    }
}

@Composable
private fun MatchesTab(viewModel: ClubDetailViewModel, onBoxOffice: (MatchDto) -> Unit, onEdit: (MatchDto) -> Unit, onDelete: (MatchDto) -> Unit) {
    val matches = viewModel.matches
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Subtitle(stringResource(R.string.club_matches_subtitle)) }
        if (matches.isEmpty()) {
            item { EmptySection(stringResource(R.string.club_no_matches)) }
        } else {
            items(matches) { match ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(formatKickoff(match.kickoffAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(matchLabel(match, viewModel::clubName, viewModel::teamCategory), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        viewModel.venueName(match.venueId)?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(match.status)
                            if (match.homeScore != null && match.awayScore != null) {
                                Text("${match.homeScore} – ${match.awayScore}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEdit(match) }) { Icon(Icons.Filled.Edit, stringResource(R.string.common_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            IconButton(onClick = { onDelete(match) }) { Icon(Icons.Filled.Delete, stringResource(R.string.common_delete), tint = MaterialTheme.colorScheme.error) }
                            Button(onClick = { onBoxOffice(match) }) {
                                Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                                Text(stringResource(R.string.match_box_office))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetsTab(viewModel: ClubDetailViewModel, onMatchSheetClick: (Long) -> Unit) {
    val matches = viewModel.matches
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Subtitle(stringResource(R.string.club_sheets_subtitle)) }
        if (matches.isEmpty()) {
            item { EmptySection(stringResource(R.string.club_no_matches)) }
        } else {
            items(matches) { match ->
                Card(onClick = { onMatchSheetClick(match.id) }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(formatKickoff(match.kickoffAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(matchLabel(match, viewModel::clubName, viewModel::teamCategory), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersTab(members: MembersDto?, onAdd: () -> Unit, onRemove: (MemberDto, String) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Subtitle(stringResource(R.string.club_members_subtitle)) }
            val managers = members?.managers.orEmpty()
            val staff = members?.staff.orEmpty()
            item { SectionLabel(stringResource(R.string.club_managers)) }
            if (managers.isEmpty()) item { EmptySection(stringResource(R.string.club_no_members)) }
            else items(managers) { MemberRow(it) { onRemove(it, "MANAGER") } }
            item { SectionLabel(stringResource(R.string.club_staff)) }
            if (staff.isEmpty()) item { EmptySection(stringResource(R.string.club_no_members)) }
            else items(staff) { MemberRow(it) { onRemove(it, "STAFF") } }
        }
        Fab(onAdd, stringResource(R.string.member_add))
    }
}

@Composable
private fun MemberRow(member: MemberDto, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                Text(memberName(member), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                val sub = listOfNotNull(member.username?.let { "@$it" }, member.email).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, stringResource(R.string.common_remove), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EditClubDialog(club: ClubDto, onSave: (String, Int?, String?) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(club.name) }
    var year by remember { mutableStateOf(club.foundedYear?.toString() ?: "") }
    var crest by remember { mutableStateOf(club.crestUrl ?: "") }
    FormDialog(
        title = stringResource(R.string.club_edit_title),
        confirmLabel = stringResource(R.string.common_save),
        confirmEnabled = name.trim().length >= 2,
        onConfirm = { onSave(name.trim(), year.toIntOrNull(), crest.trim()) },
        onDismiss = onDismiss,
    ) {
        Field(name, { name = it }, R.string.club_name)
        Field(year, { year = it.filter { c -> c.isDigit() } }, R.string.club_founded_label, KeyboardType.Number)
        Field(crest, { crest = it }, R.string.club_crest_url)
    }
}

@Composable
private fun BoxOfficeDialog(onOpen: (String, Double, Double) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var normal by remember { mutableStateOf("8.00") }
    var supporter by remember { mutableStateOf("4.00") }
    FormDialog(
        title = stringResource(R.string.match_box_office_title),
        confirmLabel = stringResource(R.string.box_office_open),
        confirmEnabled = name.isNotBlank(),
        onConfirm = { onOpen(name.trim(), normal.toDoubleOrNull() ?: 0.0, supporter.toDoubleOrNull() ?: 0.0) },
        onDismiss = onDismiss,
    ) {
        Field(name, { name = it }, R.string.match_event_name)
        Field(normal, { normal = it }, R.string.match_price_normal, KeyboardType.Decimal)
        Field(supporter, { supporter = it }, R.string.match_price_supporter, KeyboardType.Decimal)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMatchDialog(match: MatchDto, venues: List<VenueDto>, onSave: (Long?, String, String?) -> Unit, onDismiss: () -> Unit) {
    var date by remember { mutableStateOf(isoToLocalDate(match.kickoffAt)) }
    var time by remember { mutableStateOf(isoToLocalTime(match.kickoffAt)) }
    var referee by remember { mutableStateOf(match.refereeName ?: "") }
    var venueId by remember { mutableStateOf(match.venueId) }
    var menuOpen by remember { mutableStateOf(false) }
    var invalid by remember { mutableStateOf(false) }

    FormDialog(
        title = stringResource(R.string.match_edit_title),
        confirmLabel = stringResource(R.string.common_save),
        confirmEnabled = date.isNotBlank() && time.isNotBlank(),
        onConfirm = {
            val iso = localDateTimeToIso(date.trim(), time.trim())
            if (iso == null) invalid = true else onSave(venueId, iso, referee.trim().ifBlank { null })
        },
        onDismiss = onDismiss,
    ) {
        Text(stringResource(R.string.ed_venue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    venues.firstOrNull { it.id == venueId }?.name ?: stringResource(R.string.match_no_venue),
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.match_no_venue)) }, onClick = { venueId = null; menuOpen = false })
                venues.forEach { v ->
                    DropdownMenuItem(text = { Text(v.name) }, onClick = { venueId = v.id; menuOpen = false })
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(date, { date = it; invalid = false }, label = { Text(stringResource(R.string.match_date_label)) }, placeholder = { Text(stringResource(R.string.match_date_hint)) }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(time, { time = it; invalid = false }, label = { Text(stringResource(R.string.match_time_label)) }, placeholder = { Text(stringResource(R.string.match_time_hint)) }, singleLine = true, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(referee, { referee = it }, label = { Text(stringResource(R.string.match_referee)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        if (invalid) Text(stringResource(R.string.match_datetime_invalid), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun CategoryDialog(title: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                TEAM_CATEGORIES.forEach { opt ->
                    Text(
                        prettyCategory(opt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(opt) }.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMemberDialog(
    isAdmin: Boolean,
    searchResults: List<UserDto>,
    searching: Boolean,
    onSearch: (String) -> Unit,
    onAddExisting: (String, String) -> Unit,
    onAddNew: (String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var role by remember { mutableStateOf("STAFF") }
    var tab by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.member_add_title), style = MaterialTheme.typography.titleMedium)

                if (isAdmin) {
                    Text(stringResource(R.string.member_role), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CLUB_ROLES.forEach { r -> FilterChip(selected = role == r, onClick = { role = r }, label = { Text(prettyClubRole(r)) }) }
                    }
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.member_tab_existing)) })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.member_tab_new)) })
                    }
                }

                if (isAdmin && tab == 0) {
                    ExistingUserPanel(searchResults, searching, onSearch) { userId -> onAddExisting(userId, role) }
                } else {
                    NewUserPanel { u, e, f, l, p -> onAddNew(u, e, f, l, p, role) }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
                }
            }
        }
    }
}

@Composable
private fun ExistingUserPanel(searchResults: List<UserDto>, searching: Boolean, onSearch: (String) -> Unit, onAdd: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    Field(query, { query = it }, R.string.member_search_hint)
    Button(onClick = { onSearch(query) }, enabled = query.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.member_search))
    }
    when {
        searching -> Text(stringResource(R.string.member_searching), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        searchResults.isEmpty() -> Text(stringResource(R.string.member_no_results), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> searchResults.forEach { user ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(listOfNotNull(user.firstName, user.lastName).joinToString(" ").ifBlank { user.username ?: user.id }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    val sub = listOfNotNull(user.username?.let { "@$it" }, user.email).joinToString(" · ")
                    if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { onAdd(user.id) }) { Text(stringResource(R.string.member_add_existing)) }
            }
        }
    }
}

@Composable
private fun NewUserPanel(onAddNew: (String, String, String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Field(username, { username = it }, R.string.member_username)
    Field(email, { email = it }, R.string.member_email)
    Field(firstName, { firstName = it }, R.string.player_first)
    Field(lastName, { lastName = it }, R.string.player_last)
    Field(password, { password = it }, R.string.member_password)
    Text(stringResource(R.string.member_password_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(
        onClick = { onAddNew(username.trim(), email.trim(), firstName.trim(), lastName.trim(), password) },
        enabled = username.trim().length >= 3 && firstName.isNotBlank() && lastName.isNotBlank() && password.length >= 6,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.member_create_add)) }
}

private fun matchLabel(m: MatchDto, clubName: (Long?) -> String?, teamCategory: (Long?) -> String?): String {
    fun side(clubId: Long?, teamId: Long?): String {
        val c = clubName(clubId) ?: "?"
        val cat = teamCategory(teamId)?.let { prettyCategory(it) }
        return if (cat != null) "$c ($cat)" else c
    }
    return "${side(m.homeClubId, m.homeTeamId)} vs ${side(m.awayClubId, m.awayTeamId)}"
}

private fun memberName(member: MemberDto): String =
    listOfNotNull(member.firstName, member.lastName).joinToString(" ").ifBlank { member.username ?: member.userId }

@Composable
private fun Field(value: String, onChange: (String) -> Unit, labelRes: Int, keyboard: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(labelRes)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Fab(onClick: () -> Unit, desc: String) {
    Box(Modifier.fillMaxSize()) {
        FloatingActionButton(onClick = onClick, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = desc)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun Subtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = GlassInkMuted)
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = GlassInkMuted, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun EmptySection(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = GlassInkMuted, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}
