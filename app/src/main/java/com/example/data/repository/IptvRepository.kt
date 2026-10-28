package com.example.data.repository

import com.example.data.local.ChannelDao
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.model.Channel
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType
import com.example.data.model.WatchHistoryItem
import com.example.data.service.M3UPlaylistService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class IptvRepository(
    private val channelDao: ChannelDao,
    private val playlistDao: PlaylistDao,
    val m3uPlaylistService: M3UPlaylistService = M3UPlaylistService(channelDao, playlistDao),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val defaultPlaylist = Playlist(
        id = "default_curated_playlist",
        name = "Aura Premium 4K Showcase",
        sourceUrl = "https://aura-iptv.local/curated.m3u",
        type = PlaylistType.CURATED_DEFAULT,
        channelCount = SampleChannels.curatedChannels.size,
        isActive = true
    )

    val playlists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
        .map { entities ->
            if (entities.isEmpty()) listOf(defaultPlaylist)
            else entities.map { it.toPlaylist() }
        }

    val allChannels: Flow<List<Channel>> = channelDao.getAllChannels()
        .map { entities ->
            if (entities.isEmpty()) SampleChannels.curatedChannels
            else entities.map { it.toChannel() }
        }

    val favoriteIds: Flow<Set<String>> = channelDao.getFavoriteChannels()
        .map { entities -> entities.map { it.id }.toSet() }

    private val _watchHistory = MutableStateFlow<List<WatchHistoryItem>>(
        listOf(
            WatchHistoryItem(
                channelId = "chan_tears_of_steel_4k",
                channelName = "Tears of Steel (Sci-Fi 4K)",
                streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?q=80&w=300&auto=format&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?q=80&w=1200&auto=format&fit=crop",
                progressFraction = 0.62f,
                category = "Cinema 4K"
            ),
            WatchHistoryItem(
                channelId = "chan_nasa_4k",
                channelName = "NASA TV Ultra HD",
                streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
                logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
                backdropUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200&auto=format&fit=crop",
                progressFraction = 0.88f,
                category = "Documentaries"
            ),
            WatchHistoryItem(
                channelId = "chan_redbull_sports",
                channelName = "Red Bull Extreme Sports",
                streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
                logoUrl = "https://images.unsplash.com/photo-1517649763962-0c623266ddc0?q=80&w=300&auto=format&fit=crop",
                backdropUrl = "https://images.unsplash.com/photo-1551698618-1dfe5d97d256?q=80&w=1200&auto=format&fit=crop",
                progressFraction = 0.35f,
                category = "Sports & Live"
            )
        )
    )
    val watchHistory: StateFlow<List<WatchHistoryItem>> = _watchHistory.asStateFlow()

    init {
        coroutineScope.launch {
            seedInitialDataIfEmpty()
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (channelDao.getChannelCount() == 0) {
            playlistDao.insertPlaylist(PlaylistEntity.fromPlaylist(defaultPlaylist))
            val entities = SampleChannels.curatedChannels.map {
                ChannelEntity.fromChannel(it, defaultPlaylist.id)
            }
            channelDao.insertChannels(entities)
        }
    }

    suspend fun toggleFavorite(channelId: String) = withContext(Dispatchers.IO) {
        val existing = channelDao.getChannelById(channelId)
        if (existing != null) {
            channelDao.updateFavorite(channelId, !existing.isFavorite)
        }
    }

    fun recordWatchProgress(channel: Channel, progressFraction: Float = 0.1f) {
        val existing = _watchHistory.value.filter { it.channelId != channel.id }
        val updated = listOf(
            WatchHistoryItem(
                channelId = channel.id,
                channelName = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                backdropUrl = channel.backdropUrl.ifBlank { "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=1200&auto=format&fit=crop" },
                timestamp = System.currentTimeMillis(),
                progressFraction = progressFraction,
                category = channel.category
            )
        ) + existing
        _watchHistory.value = updated.take(15)
    }

    suspend fun addPlaylistFromUrl(name: String, url: String): Result<Playlist> = withContext(Dispatchers.IO) {
        val playlistId = UUID.randomUUID().toString()
        val result = m3uPlaylistService.parseFromUrlAndStore(
            url = url,
            playlistId = playlistId,
            playlistName = name.ifBlank { "Custom Playlist" }
        )
        result.map { channels ->
            Playlist(
                id = playlistId,
                name = name.ifBlank { "Custom Playlist" },
                sourceUrl = url,
                type = PlaylistType.M3U_URL,
                channelCount = channels.size,
                lastUpdated = System.currentTimeMillis(),
                isActive = true
            )
        }
    }

    suspend fun addPlaylistFromRawText(name: String, m3uText: String): Result<Playlist> = withContext(Dispatchers.IO) {
        val playlistId = UUID.randomUUID().toString()
        val result = m3uPlaylistService.parseAndStore(
            m3uContent = m3uText,
            playlistId = playlistId,
            playlistName = name.ifBlank { "Imported Playlist" },
            sourceUrl = "Direct Text Input",
            playlistType = "M3U_TEXT"
        )
        result.map { channels ->
            Playlist(
                id = playlistId,
                name = name.ifBlank { "Imported Playlist" },
                sourceUrl = "Direct Text Input",
                type = PlaylistType.M3U_TEXT,
                channelCount = channels.size,
                lastUpdated = System.currentTimeMillis(),
                isActive = true
            )
        }
    }

    suspend fun addXtreamCodesPlaylist(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        val cleanServer = serverUrl.trimEnd('/')
        val m3uEndpoint = "$cleanServer/get.php?username=$username&password=$password&type=m3u_plus&output=m3u8"
        addPlaylistFromUrl(name.ifBlank { "Xtream: $username" }, m3uEndpoint)
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        if (playlistId == defaultPlaylist.id) return@withContext
        playlistDao.deletePlaylistById(playlistId)
        channelDao.deleteChannelsByPlaylist(playlistId)
    }
}
