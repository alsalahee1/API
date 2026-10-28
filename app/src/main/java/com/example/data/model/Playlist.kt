package com.example.data.model

enum class PlaylistType {
    M3U_URL,
    M3U_TEXT,
    XTREAM_CODES,
    CURATED_DEFAULT
}

data class Playlist(
    val id: String,
    val name: String,
    val sourceUrl: String = "",
    val type: PlaylistType = PlaylistType.M3U_URL,
    val channelCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class MultiStreamSlot(
    val slotIndex: Int,
    val channel: Channel? = null,
    val isMuted: Boolean = false,
    val isSelected: Boolean = false
)

data class WatchHistoryItem(
    val channelId: String,
    val channelName: String,
    val streamUrl: String,
    val logoUrl: String,
    val backdropUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val progressFraction: Float = 0.5f,
    val category: String = "Entertainment"
)
