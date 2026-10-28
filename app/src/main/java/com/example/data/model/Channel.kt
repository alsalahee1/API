package com.example.data.model

enum class StreamType {
    LIVE_TV,
    MOVIE,
    SERIES
}

data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val category: String = "Entertainment",
    val groupTitle: String = "General",
    val tvgId: String = "",
    val resolution: String = "1080p FHD",
    val isFavorite: Boolean = false,
    val isLive: Boolean = true,
    val currentProgram: String = "Live Broadcast",
    val nextProgram: String = "Upcoming Show",
    val description: String = "High definition live broadcast stream.",
    val backdropUrl: String = "",
    val rating: String = "TV-14",
    val streamType: StreamType = StreamType.LIVE_TV
)
