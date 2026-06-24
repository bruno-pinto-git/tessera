package com.tessera.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.foundation.layout.padding
import com.tessera.android.R
import com.tessera.android.screens.ClubDetailScreen
import com.tessera.android.screens.ClubsAdminScreen
import com.tessera.android.screens.ClubsListScreen
import com.tessera.android.screens.EventDetailScreen
import com.tessera.android.screens.EventsScreen
import com.tessera.android.screens.MatchSheetScreen
import com.tessera.android.screens.MyTicketsScreen
import com.tessera.android.screens.SettingsScreen
import com.tessera.android.screens.TeamDetailScreen
import com.tessera.android.screens.UsersAdminScreen
import com.tessera.android.screens.ValidateScreen
import com.tessera.android.screens.VenuesAdminScreen
import com.tessera.android.shared.AuthSession
import com.tessera.android.shared.Capability
import com.tessera.android.shared.Permissions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(onRequestLogin: () -> Unit, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val baseRoute = currentRoute?.substringBefore("/")
    val isTopLevel = baseRoute in TOP_LEVEL_ROUTES
    val start = remember { DRAWER_ITEMS.firstOrNull { Permissions.can(it.capability) }?.route ?: Routes.SETTINGS }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = baseRoute,
                onItemClick = { route ->
                    scope.launch { drawerState.close() }
                    nav.navigate(route) {
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    nav.navigate(Routes.SETTINGS) { launchSingleTop = true }
                },
                onLogout = onLogout,
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(titleResForRoute(currentRoute))) },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_open))
                            }
                        } else {
                            IconButton(onClick = { nav.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                            }
                        }
                    },
                    actions = {
                        if (!AuthSession.isAuthenticated) {
                            Button(
                                onClick = onRequestLogin,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                modifier = Modifier.padding(end = 8.dp),
                            ) {
                                Text(stringResource(R.string.menu_login))
                            }
                        }
                    },
                )
            },
        ) { inner ->
            NavHost(
                navController = nav,
                startDestination = start,
                modifier = Modifier.padding(inner),
            ) {
                composable(Routes.EVENTS) {
                    EventsScreen(onEventClick = { id -> nav.navigate("${Routes.EVENT_DETAIL}/$id") })
                }
                composable(
                    route = "${Routes.EVENT_DETAIL}/{eventId}",
                    arguments = listOf(navArgument("eventId") { type = NavType.LongType }),
                ) { entry ->
                    EventDetailScreen(
                        eventId = entry.arguments?.getLong("eventId") ?: 0L,
                        onOpenTickets = {
                            nav.navigate(Routes.MY_TICKETS) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onRequestLogin = onRequestLogin,
                    )
                }
                composable(Routes.MY_TICKETS) {
                    MyTicketsScreen(onOpenEvent = { id -> nav.navigate("${Routes.EVENT_DETAIL}/$id") })
                }
                composable(Routes.VALIDATE) { ValidateScreen() }
                composable(Routes.CLUBS) {
                    ClubsListScreen(onClubClick = { id -> nav.navigate("${Routes.CLUB_DETAIL}/$id") })
                }
                composable(
                    route = "${Routes.CLUB_DETAIL}/{clubId}",
                    arguments = listOf(navArgument("clubId") { type = NavType.LongType }),
                ) { entry ->
                    ClubDetailScreen(
                        clubId = entry.arguments?.getLong("clubId") ?: 0L,
                        onTeamClick = { id -> nav.navigate("${Routes.TEAM_DETAIL}/$id") },
                        onMatchSheetClick = { id -> nav.navigate("${Routes.MATCH_SHEET}/$id") },
                        isAdmin = Permissions.can(Capability.ADMIN),
                    )
                }
                composable(
                    route = "${Routes.MATCH_SHEET}/{matchId}",
                    arguments = listOf(navArgument("matchId") { type = NavType.LongType }),
                ) { entry ->
                    MatchSheetScreen(
                        matchId = entry.arguments?.getLong("matchId") ?: 0L,
                        isAdmin = Permissions.can(Capability.ADMIN),
                    )
                }
                composable(
                    route = "${Routes.TEAM_DETAIL}/{teamId}",
                    arguments = listOf(navArgument("teamId") { type = NavType.LongType }),
                ) { entry ->
                    TeamDetailScreen(teamId = entry.arguments?.getLong("teamId") ?: 0L)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(onBack = { nav.popBackStack() })
                }
                composable(Routes.ADMIN_CLUBS) {
                    ClubsAdminScreen(onClubClick = { id -> nav.navigate("${Routes.CLUB_DETAIL}/$id") })
                }
                composable(Routes.ADMIN_VENUES) {
                    VenuesAdminScreen()
                }
                composable(Routes.ADMIN_USERS) {
                    UsersAdminScreen()
                }
            }
        }
    }
}
