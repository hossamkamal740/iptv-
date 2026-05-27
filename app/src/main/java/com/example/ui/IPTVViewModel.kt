package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IPTVViewModel(application: Application) : AndroidViewModel(application) {
    private val tag = "IPTVViewModel"
    private val database = IPTVDatabase.getDatabase(application)
    private val repository = IPTVRepository(application, database.iptvDao())

    // UI States
    val playlists = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePlaylist = repository.activePlaylist.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _selectedGroup = MutableStateFlow("All")
    val selectedGroup: StateFlow<String> = _selectedGroup.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isParentalLockActive = MutableStateFlow(false)
    val isParentalLockActive: StateFlow<Boolean> = _isParentalLockActive.asStateFlow()

    private val _parentalPin = MutableStateFlow("1111")
    val parentalPin: StateFlow<String> = _parentalPin.asStateFlow()

    fun setParentalLock(active: Boolean) {
        _isParentalLockActive.value = active
    }

    fun setParentalPin(pin: String) {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            _parentalPin.value = pin
        }
    }

    // Combined live streams based on Active Playlist, Selected Group, and Search Query
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val channels = combine(
        selectedGroup,
        searchQuery,
        activePlaylist
    ) { group, query, playlist ->
        Triple(group, query, playlist)
    }.flatMapLatest { (group, query, playlist) ->
        if (playlist == null) {
            flowOf(emptyList())
        } else if (query.isNotEmpty()) {
            repository.searchChannels(query)
        } else {
            repository.getChannelsByGroup(group)
        }
    }.combine(isParentalLockActive) { list, lockActive ->
        if (lockActive) {
            list.filter { channel ->
                val nameLower = channel.name.lowercase()
                val groupLower = channel.groupTitle.lowercase()
                !nameLower.contains("xxx") && !nameLower.contains("18+") && !nameLower.contains("+18") && !nameLower.contains("adult") &&
                !groupLower.contains("xxx") && !groupLower.contains("18+") && !groupLower.contains("+18") && !groupLower.contains("adult")
            }
        } else {
            list
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val groups = repository.activeGroups.combine(isParentalLockActive) { list, lockActive ->
        val filtered = if (lockActive) {
            list.filter { group ->
                val groupLower = group.lowercase()
                !groupLower.contains("xxx") && !groupLower.contains("18+") && !groupLower.contains("+18") && !groupLower.contains("adult")
            }
        } else {
            list
        }
        listOf("All") + filtered.filter { it.isNotEmpty() && it != "All" }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("All")
    )

    val favorites = repository.favoriteChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val history = repository.historyChannels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamic app configurations
    private val _isDarkMode = MutableStateFlow(true) // Premium dark vibe default
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _currentLanguage = MutableStateFlow("ar") // Default to Arabic for Middle East IPTV target
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    init {
        // Try to trigger loading saved data or active playlist refresh
        viewModelScope.launch {
            activePlaylist.collect { playlist ->
                if (playlist != null) {
                    Log.d(tag, "Loaded active playlist: ${playlist.name}")
                }
            }
        }

        // Automatic default playlist onboarding for Arabic M3U
        viewModelScope.launch {
            val currentPlaylists = repository.allPlaylists.firstOrNull() ?: emptyList()
            if (currentPlaylists.isEmpty()) {
                _isLoading.value = true
                val defaultPlaylist = PlaylistEntity(
                    name = "القنوات المفتوحة (عربي)",
                    type = "m3u",
                    url = "https://iptv-org.github.io/iptv/languages/ara.m3u",
                    isActive = true
                )
                repository.addNewPlaylistDirect(defaultPlaylist)
                _isLoading.value = false
            }
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setLanguage(langCode: String) {
        if (langCode == "ar" || langCode == "en") {
            _currentLanguage.value = langCode
        }
    }

    fun selectGroup(group: String) {
        _selectedGroup.value = group
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addPlaylist(
        name: String,
        type: String, // "m3u" or "xtream"
        m3uUrl: String? = null,
        host: String? = null,
        username: String? = null,
        password: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val playlist = PlaylistEntity(
                name = name,
                type = type,
                url = m3uUrl,
                host = host,
                username = username,
                password = password,
                isActive = false
            )

            val success = repository.addNewPlaylist(playlist)
            _isLoading.value = false
            if (success) {
                _selectedGroup.value = "All"
                _searchQuery.value = ""
                onSuccess()
            } else {
                _errorMessage.value = "Failed to connect to provider. Verify link or credentials."
            }
        }
    }

    fun switchPlaylist(playlistId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.switchAndActivatePlaylist(playlistId)
            _selectedGroup.value = "All"
            _searchQuery.value = ""
            _isLoading.value = false
        }
    }

    fun refreshChannels() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = repository.refreshActivePlaylist()
            _isLoading.value = false
            if (!success) {
                _errorMessage.value = "Failed to refresh streams from server"
            }
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            // If the deleted playlist was active, activePlaylist will automatically flow null.
        }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch {
            val isFav = favorites.value.any { it.id == channel.id }
            if (isFav) {
                repository.removeFavorite(channel.id)
            } else {
                repository.addFavorite(channel.id)
            }
        }
    }

    fun isFavorite(channelId: String): Flow<Boolean> {
        return repository.isFavorite(channelId)
    }

    fun addChannelToHistory(channel: ChannelEntity) {
        viewModelScope.launch {
            repository.addHistory(channel.id)
        }
    }

    fun removeHistoryEntry(channelId: String) {
        viewModelScope.launch {
            repository.deleteHistory(channelId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearAppCache() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.clearAllCache()
            _isLoading.value = false
        }
    }
}
