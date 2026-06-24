package com.tessera.android.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tessera.android.data.ClubRepository
import com.tessera.android.data.UserRepository
import com.tessera.android.data.VenueRepository
import com.tessera.android.data.dto.ClubDto
import com.tessera.android.data.dto.MatchDto
import com.tessera.android.data.dto.MembersDto
import com.tessera.android.data.dto.TeamDto
import com.tessera.android.data.dto.UserDto
import com.tessera.android.data.dto.VenueDto
import kotlinx.coroutines.launch

class ClubDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ClubRepository(application)
    private val venues = VenueRepository(application)
    private val users = UserRepository(application)

    var club by mutableStateOf<ClubDto?>(null)
        private set
    var teams by mutableStateOf<List<TeamDto>>(emptyList())
        private set
    var matches by mutableStateOf<List<MatchDto>>(emptyList())
        private set
    var members by mutableStateOf<MembersDto?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set

    var searchResults by mutableStateOf<List<UserDto>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var venuesList by mutableStateOf<List<VenueDto>>(emptyList())
        private set

    private var clubsById: Map<Long, String> = emptyMap()
    private var venuesById: Map<Long, String> = emptyMap()
    private var teamCatById: Map<Long, String> = emptyMap()

    private var currentClubId: Long = 0

    fun load(clubId: Long) {
        currentClubId = clubId
        loading = true
        error = false
        viewModelScope.launch {
            try {
                club = repo.getClub(clubId)
                teams = repo.teams(clubId)
                matches = repo.matchesByClub(clubId)
                members = repo.members(clubId)
                loadLookups()
            } catch (e: Exception) {
                error = true
            }
            loading = false
        }
    }

    private suspend fun loadLookups() {
        runCatching { clubsById = repo.listClubs().associate { it.id to it.name } }
        runCatching {
            val v = venues.list()
            venuesList = v
            venuesById = v.associate { it.id to it.name }
        }
        val clubIds = (matches.mapNotNull { it.homeClubId } + matches.mapNotNull { it.awayClubId }).toSet()
        val cats = mutableMapOf<Long, String>()
        clubIds.forEach { cid -> runCatching { repo.teams(cid).forEach { cats[it.id] = it.category } } }
        teamCatById = cats
    }

    fun clubName(id: Long?): String? = id?.let { clubsById[it] ?: if (it == currentClubId) club?.name else null }
    fun venueName(id: Long?): String? = id?.let { venuesById[it] }
    fun teamCategory(id: Long?): String? = id?.let { teamCatById[it] }

    fun updateClub(name: String?, foundedYear: Int?, crestUrl: String?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updateClub(currentClubId, name, foundedYear, crestUrl)
                club = repo.getClub(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun createTeam(category: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.createTeam(currentClubId, category)
                teams = repo.teams(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun updateTeam(teamId: Long, category: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updateTeam(teamId, category)
                teams = repo.teams(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun deleteTeam(teamId: Long) {
        busy = true
        viewModelScope.launch {
            try {
                repo.deleteTeam(teamId)
                teams = repo.teams(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun addMember(username: String, email: String, firstName: String, lastName: String, password: String, role: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.addMember(currentClubId, username, email, firstName, lastName, password, role)
                members = repo.members(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun addExistingMember(userId: String, role: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.addExistingMember(currentClubId, userId, role)
                members = repo.members(currentClubId)
                searchResults = emptyList()
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun removeMember(userId: String, role: String) {
        busy = true
        viewModelScope.launch {
            try {
                repo.removeMember(currentClubId, userId, role)
                members = repo.members(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun searchUsers(query: String) {
        searching = true
        viewModelScope.launch {
            searchResults = try {
                users.search(query)
            } catch (e: Exception) {
                emptyList()
            }
            searching = false
        }
    }

    fun clearSearch() {
        searchResults = emptyList()
    }

    fun updateMatch(matchId: Long, venueId: Long?, kickoffAt: String, refereeName: String?) {
        busy = true
        viewModelScope.launch {
            try {
                repo.updateMatch(matchId, venueId, kickoffAt, refereeName)
                matches = repo.matchesByClub(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun deleteMatch(matchId: Long) {
        busy = true
        viewModelScope.launch {
            try {
                repo.deleteMatch(matchId)
                matches = repo.matchesByClub(currentClubId)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }

    fun openBoxOffice(matchId: Long, name: String, priceNormal: Double, priceSupporter: Double) {
        busy = true
        viewModelScope.launch {
            try {
                repo.openBoxOffice(matchId, name, priceNormal, priceSupporter)
            } catch (e: Exception) {
                error = true
            }
            busy = false
        }
    }
}
