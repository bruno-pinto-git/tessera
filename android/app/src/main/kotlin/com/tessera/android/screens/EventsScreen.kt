package com.tessera.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.CatalogEntry
import com.tessera.android.screens.components.ClubCrest
import com.tessera.android.screens.components.formatEur
import com.tessera.android.screens.components.formatKickoffFull
import com.tessera.android.screens.components.kickoffParts
import com.tessera.android.screens.components.RefreshOnResume
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.EventsFilter
import com.tessera.android.viewmodels.EventsState
import com.tessera.android.viewmodels.EventsViewModel

@Composable
fun EventsScreen(
    onEventClick: (Long) -> Unit,
    viewModel: EventsViewModel = viewModel(),
) {
    RefreshOnResume { viewModel.load() }
    when (val s = viewModel.state) {
        EventsState.Loading -> Centered {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.events_loading), color = GlassInkMuted)
        }
        EventsState.Error -> Centered {
            Text(stringResource(R.string.events_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
        }
        is EventsState.Success -> EventsList(s.all, viewModel, onEventClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventsList(
    all: List<CatalogEntry>,
    viewModel: EventsViewModel,
    onEventClick: (Long) -> Unit,
) {
    val visible = viewModel.visible(all)
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.events_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = GlassInkMuted,
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipFor(EventsFilter.ALL, R.string.events_filter_all, viewModel)
                FilterChipFor(EventsFilter.UPCOMING, R.string.events_filter_upcoming, viewModel)
                FilterChipFor(EventsFilter.FINISHED, R.string.events_filter_finished, viewModel)
            }
        }
        when {
            all.isEmpty() -> item { EmptyHint(stringResource(R.string.events_empty)) }
            visible.isEmpty() -> item { EmptyHint(stringResource(R.string.events_empty_filtered)) }
            else -> items(visible, key = { it.eventId }) { entry ->
                MatchCard(entry) { onEventClick(entry.eventId) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipFor(
    filter: EventsFilter,
    labelRes: Int,
    viewModel: EventsViewModel,
) {
    FilterChip(
        selected = viewModel.filter == filter,
        onClick = { viewModel.updateFilter(filter) },
        label = { Text(stringResource(labelRes)) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchCard(entry: CatalogEntry, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CrestSide(entry.homeInitials, entry.homeShort, stringResource(R.string.match_home), entry.homeTone, Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.match_vs), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    kickoffParts(entry.kickoffAt)?.let {
                        Text(it.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CrestSide(entry.awayInitials, entry.awayShort, stringResource(R.string.match_away), entry.awayTone, Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MetaRow(Icons.Filled.DateRange, formatKickoffFull(entry.kickoffAt))
                entry.venueName?.let { MetaRow(Icons.Filled.Place, it) }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    if (entry.hasResult) {
                        Text(stringResource(R.string.events_result), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${entry.homeScore} – ${entry.awayScore}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    } else {
                        Text(stringResource(R.string.events_from), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatEur(entry.priceFrom), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(
                    text = "${stringResource(R.string.events_see_details)} →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun CrestSide(initials: String, short: String, label: String, tone: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ClubCrest(initials, tone, 52.dp)
        Text(
            text = short,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MetaRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = GlassInkMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}
