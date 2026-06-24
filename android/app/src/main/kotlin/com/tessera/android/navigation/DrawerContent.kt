package com.tessera.android.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tessera.android.R
import com.tessera.android.shared.AuthSession
import com.tessera.android.shared.Permissions

@Composable
fun DrawerContent(
    currentRoute: String?,
    onItemClick: (String) -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val authenticated = AuthSession.isAuthenticated
    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.8f)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(20.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_tessera_logo),
                    contentDescription = stringResource(R.string.welcome_logo_desc),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (authenticated) (AuthSession.username ?: stringResource(R.string.app_name)) else stringResource(R.string.menu_guest),
                    style = MaterialTheme.typography.titleMedium,
                )
                if (authenticated) {
                    AuthSession.roles.firstOrNull()?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            val items = DRAWER_ITEMS.filter { Permissions.can(it.capability) }
            DrawerSection.entries.forEach { section ->
                val secItems = items.filter { it.section == section }
                if (secItems.isNotEmpty()) {
                    Text(
                        text = stringResource(section.labelRes).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp, top = 10.dp, bottom = 4.dp),
                    )
                    secItems.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(item.labelRes)) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = currentRoute == item.route,
                            onClick = { onItemClick(item.route) },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.menu_settings)) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = currentRoute == Routes.SETTINGS,
                onClick = onSettings,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
            if (authenticated) {
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.menu_logout)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                    selected = false,
                    onClick = onLogout,
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = MaterialTheme.colorScheme.error,
                        unselectedTextColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
