package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Channel
import com.example.data.model.StreamType

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
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
    val description: String = "",
    val backdropUrl: String = "",
    val rating: String = "TV-14",
    val streamType: String = "LIVE_TV",
    val playlistId: String = "default",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toChannel(): Channel {
        val sType = try {
            StreamType.valueOf(streamType)
        } catch (e: Exception) {
            StreamType.LIVE_TV
        }

        return Channel(
            id = id,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            category = category,
            groupTitle = groupTitle,
            tvgId = tvgId,
            resolution = resolution,
            isFavorite = isFavorite,
            isLive = isLive,
            currentProgram = currentProgram,
            nextProgram = nextProgram,
            description = description,
            backdropUrl = backdropUrl,
            rating = rating,
            streamType = sType
        )
    }

    companion object {
        fun fromChannel(channel: Channel, playlistId: String = "default"): ChannelEntity {
            return ChannelEntity(
                id = channel.id,
                name = channel.name,
                streamUrl = channel.streamUrl,
                logoUrl = channel.logoUrl,
                category = channel.category,
                groupTitle = channel.groupTitle,
                tvgId = channel.tvgId,
                resolution = channel.resolution,
                isFavorite = channel.isFavorite,
                isLive = channel.isLive,
                currentProgram = channel.currentProgram,
                nextProgram = channel.nextProgram,
                description = channel.description,
                backdropUrl = channel.backdropUrl,
                rating = channel.rating,
                streamType = channel.streamType.name,
                playlistId = playlistId
            )
        }
    }
}
