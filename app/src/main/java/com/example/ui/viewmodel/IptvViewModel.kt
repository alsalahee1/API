package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Channel
import com.example.data.model.MultiStreamSlot
import com.example.data.model.Playlist
import com.example.data.model.WatchHistoryItem
import com.example.data.repository.IptvRepository
import com.example.data.repository.SampleChannels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenDestination {
    HOME,
    LIVE_CHANNELS,
    MULTI_STREAM,
    PLAYLISTS,
    SEARCH,
    PLAYER
}

enum class AspectRatioMode {
    FIT,
    FILL,
    ZOOM
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val id: Long = System.currentTimeMillis()
)

class IptvViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: IptvRepository = run {
        val db = AppDatabase.getInstance(application)
        IptvRepository(db.channelDao(), db.playlistDao())
    }
) : AndroidViewModel(application) {

    private val _currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeChannel = MutableStateFlow<Channel?>(null)
    val activeChannel: StateFlow<Channel?> = _activeChannel.asStateFlow()

    private val _aspectRatioMode = MutableStateFlow(AspectRatioMode.FIT)
    val aspectRatioMode: StateFlow<AspectRatioMode> = _aspectRatioMode.asStateFlow()

    // Multi-stream state
    private val _multiStreamCount = MutableStateFlow(4) // 2 or 4
    val multiStreamCount: StateFlow<Int> = _multiStreamCount.asStateFlow()

    private val _activeSlotIndex = MutableStateFlow(0)
    val activeSlotIndex: StateFlow<Int> = _activeSlotIndex.asStateFlow()

    private val _multiStreamSlots = MutableStateFlow<List<MultiStreamSlot>>(emptyList())
    val multiStreamSlots: StateFlow<List<MultiStreamSlot>> = _multiStreamSlots.asStateFlow()

    private val _isTvMode = MutableStateFlow(false)
    val isTvMode: StateFlow<Boolean> = _isTvMode.asStateFlow()

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _isAddPlaylistDialogOpen = MutableStateFlow(false)
    val isAddPlaylistDialogOpen: StateFlow<Boolean> = _isAddPlaylistDialogOpen.asStateFlow()

    private val _isAddingPlaylist = MutableStateFlow(false)
    val isAddingPlaylist: StateFlow<Boolean> = _isAddingPlaylist.asStateFlow()

    val playlists: StateFlow<List<Playlist>> = repository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(repository.defaultPlaylist))

    val allChannels: StateFlow<List<Channel>> = repository.allChannels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SampleChannels.curatedChannels)

    val favoriteIds: StateFlow<Set<String>> = repository.favoriteIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val watchHistory: StateFlow<List<WatchHistoryItem>> = repository.watchHistory

    // Filtered channels based on category, search, and favorites
    val filteredChannels: StateFlow<List<Channel>> = combine(
        allChannels,
        _selectedCategory,
        _searchQuery,
        favoriteIds
    ) { channels, category, query, favorites ->
        var list = channels
        if (category == "Favorites") {
            list = list.filter { favorites.contains(it.id) }
        } else if (category != "All") {
            list = list.filter { it.category.equals(category, ignoreCase = true) || it.groupTitle.contains(category, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.resolution.lowercase().contains(q) ||
                it.currentProgram.lowercase().contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct categories
    val categories: StateFlow<List<String>> = allChannels.combine(favoriteIds) { channels, _ ->
        val list = mutableListOf("All", "Favorites", "Cinema 4K", "Sports & Live", "News", "Documentaries", "Music", "Series")
        val custom = channels.map { it.category }.distinct()
        for (c in custom) {
            if (!list.contains(c)) list.add(c)
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Favorites", "Cinema 4K", "Sports & Live", "News", "Documentaries"))

    init {
        // Initialize multi-stream slots with top channels
        val initialChannels = SampleChannels.curatedChannels
        _multiStreamSlots.value = listOf(
            MultiStreamSlot(0, initialChannels.getOrNull(0), isMuted = false, isSelected = true),
            MultiStreamSlot(1, initialChannels.getOrNull(1), isMuted = true, isSelected = false),
            MultiStreamSlot(2, initialChannels.getOrNull(2), isMuted = true, isSelected = false),
            MultiStreamSlot(3, initialChannels.getOrNull(4), isMuted = true, isSelected = false)
        )
    }

    fun navigateTo(destination: ScreenDestination) {
        _currentScreen.value = destination
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playChannel(channel: Channel) {
        _activeChannel.value = channel
        repository.recordWatchProgress(channel)
        _currentScreen.value = ScreenDestination.PLAYER
    }

    fun closePlayer() {
        _activeChannel.value = null
        _currentScreen.value = ScreenDestination.HOME
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(channelId)
        }
    }

    fun cycleAspectRatio() {
        _aspectRatioMode.value = when (_aspectRatioMode.value) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.FIT
        }
    }

    fun setTvMode(enabled: Boolean) {
        _isTvMode.value = enabled
    }

    fun toggleAddPlaylistDialog(open: Boolean) {
        _isAddPlaylistDialogOpen.value = open
    }

    fun clearNotification() {
        _notification.value = null
    }

    // Multi-stream management
    fun setMultiStreamCount(count: Int) {
        _multiStreamCount.value = count
    }

    fun selectMultiStreamSlot(index: Int) {
        _activeSlotIndex.value = index
        _multiStreamSlots.value = _multiStreamSlots.value.map { slot ->
            slot.copy(
                isSelected = slot.slotIndex == index,
                isMuted = slot.slotIndex != index // Unmute only active focused slot
            )
        }
    }

    fun setChannelForSlot(slotIndex: Int, channel: Channel) {
        _multiStreamSlots.value = _multiStreamSlots.value.map { slot ->
            if (slot.slotIndex == slotIndex) {
                slot.copy(channel = channel)
            } else slot
        }
        _notification.value = UiNotification("Stream ${slotIndex + 1} updated to ${channel.name}")
    }

    fun toggleSlotMute(slotIndex: Int) {
        _multiStreamSlots.value = _multiStreamSlots.value.map { slot ->
            if (slot.slotIndex == slotIndex) {
                slot.copy(isMuted = !slot.isMuted)
            } else slot
        }
    }

    // Playlist imports & validation
    fun validateUrlSyntax(url: String): com.example.data.service.M3UValidationResult {
        return repository.m3uPlaylistService.validateUrlSyntax(url)
    }

    suspend fun validateRemoteM3UUrl(url: String): com.example.data.service.M3UValidationResult {
        return repository.m3uPlaylistService.validateRemoteM3UUrl(url)
    }

    fun addPlaylistUrl(name: String, url: String) {
        if (url.isBlank()) {
            _notification.value = UiNotification("Please enter a valid M3U stream URL", isError = true)
            return
        }
        viewModelScope.launch {
            _isAddingPlaylist.value = true
            val result = repository.addPlaylistFromUrl(name, url)
            _isAddingPlaylist.value = false
            if (result.isSuccess) {
                val p = result.getOrNull()
                _notification.value = UiNotification("Imported '${p?.name}' with ${p?.channelCount} channels into Room!")
                _isAddPlaylistDialogOpen.value = false
            } else {
                _notification.value = UiNotification("Error loading playlist: ${result.exceptionOrNull()?.localizedMessage}", isError = true)
            }
        }
    }

    fun addPlaylistText(name: String, text: String) {
        if (text.isBlank()) {
            _notification.value = UiNotification("Please paste M3U playlist text", isError = true)
            return
        }
        viewModelScope.launch {
            _isAddingPlaylist.value = true
            val result = repository.addPlaylistFromRawText(name, text)
            _isAddingPlaylist.value = false
            if (result.isSuccess) {
                val p = result.getOrNull()
                _notification.value = UiNotification("Imported '${p?.name}' with ${p?.channelCount} channels into Room!")
                _isAddPlaylistDialogOpen.value = false
            } else {
                _notification.value = UiNotification("Invalid M3U text: ${result.exceptionOrNull()?.localizedMessage}", isError = true)
            }
        }
    }

    fun addXtreamPlaylist(name: String, server: String, user: String, pass: String) {
        if (server.isBlank() || user.isBlank()) {
            _notification.value = UiNotification("Server URL and username are required", isError = true)
            return
        }
        viewModelScope.launch {
            _isAddingPlaylist.value = true
            val result = repository.addXtreamCodesPlaylist(name, server, user, pass)
            _isAddingPlaylist.value = false
            if (result.isSuccess) {
                val p = result.getOrNull()
                _notification.value = UiNotification("Xtream playlist connected: ${p?.channelCount} streams saved into Room!")
                _isAddPlaylistDialogOpen.value = false
            } else {
                _notification.value = UiNotification("Xtream connection error: ${result.exceptionOrNull()?.localizedMessage}", isError = true)
            }
        }
    }

    fun deletePlaylist(id: String) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
            _notification.value = UiNotification("Playlist removed from local database")
        }
    }
}
