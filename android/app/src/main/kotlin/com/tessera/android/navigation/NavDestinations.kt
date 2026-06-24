package com.tessera.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stadium
import androidx.compose.ui.graphics.vector.ImageVector
import com.tessera.android.R
import com.tessera.android.shared.Capability

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val SHELL = "shell"

    const val EVENTS = "events"
    const val EVENT_DETAIL = "event"
    const val MY_TICKETS = "my_tickets"
    const val VALIDATE = "validate"
    const val CLUBS = "clubs"
    const val CLUB_DETAIL = "club"
    const val TEAM_DETAIL = "team"
    const val MATCH_SHEET = "match_sheet"
    const val SETTINGS = "settings"
    const val ADMIN_CLUBS = "admin_clubs"
    const val ADMIN_VENUES = "admin_venues"
    const val ADMIN_USERS = "admin_users"
}

enum class DrawerSection(val labelRes: Int) {
    TICKETING(R.string.sec_ticketing),
    GATE(R.string.sec_gate),
    CLUB(R.string.sec_club),
    ADMIN(R.string.sec_admin),
}

data class DrawerItem(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val capability: Capability,
    val section: DrawerSection,
)

val DRAWER_ITEMS = listOf(
    DrawerItem(Routes.EVENTS, R.string.menu_events, Icons.Filled.Event, Capability.BROWSE_EVENTS, DrawerSection.TICKETING),
    DrawerItem(Routes.MY_TICKETS, R.string.menu_my_tickets, Icons.Filled.ConfirmationNumber, Capability.VIEW_OWN_TICKETS, DrawerSection.TICKETING),
    DrawerItem(Routes.VALIDATE, R.string.menu_validate_tickets, Icons.Filled.QrCodeScanner, Capability.VALIDATE_TICKETS, DrawerSection.GATE),
    DrawerItem(Routes.CLUBS, R.string.menu_manage_club, Icons.Filled.Groups, Capability.MANAGE_CLUB, DrawerSection.CLUB),
    DrawerItem(Routes.ADMIN_CLUBS, R.string.admin_clubs, Icons.Filled.Business, Capability.ADMIN, DrawerSection.ADMIN),
    DrawerItem(Routes.ADMIN_VENUES, R.string.admin_venues, Icons.Filled.Stadium, Capability.ADMIN, DrawerSection.ADMIN),
    DrawerItem(Routes.ADMIN_USERS, R.string.admin_users, Icons.Filled.Person, Capability.ADMIN, DrawerSection.ADMIN),
)

val TOP_LEVEL_ROUTES = setOf(
    Routes.EVENTS, Routes.MY_TICKETS, Routes.VALIDATE, Routes.CLUBS,
    Routes.ADMIN_CLUBS, Routes.ADMIN_VENUES, Routes.ADMIN_USERS,
)

fun titleResForRoute(route: String?): Int = when (route?.substringBefore("/")) {
    Routes.EVENTS -> R.string.events_title
    Routes.MY_TICKETS -> R.string.tickets_title
    Routes.VALIDATE -> R.string.validate_title
    Routes.CLUBS -> R.string.clubs_title
    Routes.CLUB_DETAIL -> R.string.clubs_title
    Routes.TEAM_DETAIL -> R.string.team_title
    Routes.MATCH_SHEET -> R.string.sheet_title
    Routes.EVENT_DETAIL -> R.string.event_detail_title
    Routes.SETTINGS -> R.string.settings_title
    Routes.ADMIN_CLUBS -> R.string.admin_clubs
    Routes.ADMIN_VENUES -> R.string.admin_venues
    Routes.ADMIN_USERS -> R.string.admin_users
    else -> R.string.app_name
}
