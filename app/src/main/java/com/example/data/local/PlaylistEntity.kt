package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceUrl: String = "",
    val type: String = "M3U_URL",
    val channelCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    fun toPlaylist(): Playlist {
        val pType = try {
            PlaylistType.valueOf(type)
        } catch (e: Exception) {
            PlaylistType.M3U_URL
        }
        return Playlist(
            id = id,
            name = name,
            sourceUrl = sourceUrl,
            type = pType,
            channelCount = channelCount,
            lastUpdated = lastUpdated,
            isActive = isActive
        )
    }

    companion object {
        fun fromPlaylist(playlist: Playlist): PlaylistEntity {
            return PlaylistEntity(
                id = playlist.id,
                name = playlist.name,
                sourceUrl = playlist.sourceUrl,
                type = playlist.type.name,
                channelCount = playlist.channelCount,
                lastUpdated = playlist.lastUpdated,
                isActive = playlist.isActive
            )
        }
    }
}
