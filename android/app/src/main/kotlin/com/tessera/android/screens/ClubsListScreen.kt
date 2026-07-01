package com.tessera.android.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tessera.android.R
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.screens.components.RefreshOnResume
import com.tessera.android.ui.theme.GlassInkMuted
import com.tessera.android.viewmodels.ClubsState
import com.tessera.android.viewmodels.ClubsViewModel

@Composable
fun ClubsListScreen(
    onClubClick: (Long) -> Unit,
    viewModel: ClubsViewModel = viewModel(),
) {
    RefreshOnResume { viewModel.load() }
    when (val s = viewModel.state) {
        ClubsState.Loading -> Centered { CircularProgressIndicator() }
        ClubsState.Error -> Centered {
            Text(stringResource(R.string.clubs_error), color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = viewModel::load) { Text(stringResource(R.string.common_retry)) }
        }
        is ClubsState.Success -> {
            if (s.clubs.isEmpty()) {
                Centered { Text(stringResource(R.string.clubs_empty), color = GlassInkMuted) }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            stringResource(R.string.clubs_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassInkMuted,
                        )
                    }
                    items(s.clubs) { club -> ClubCard(club) { onClubClick(club.id) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClubCard(club: ClubDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(club.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            club.foundedYear?.let {
                Text(stringResource(R.string.clubs_founded, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
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
