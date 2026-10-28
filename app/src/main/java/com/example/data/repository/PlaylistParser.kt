package com.example.data.repository

import com.example.data.model.Channel
import com.example.data.model.StreamType
import java.util.UUID

object PlaylistParser {

    private val TVG_ID_REGEX = """tvg-id="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = """tvg-name="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val TVG_LOGO_REGEX = """tvg-logo="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val GROUP_TITLE_REGEX = """group-title="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses standard M3U / M3U8 string into a list of Channel objects.
     */
    fun parseM3U(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = content.lines()
        var currentExtInf: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = line
            } else if (!line.startsWith("#") && currentExtInf != null) {
                // This is the stream URL line following an #EXTINF line
                val channel = parseChannelEntry(currentExtInf, line)
                if (channel != null) {
                    channels.add(channel)
                }
                currentExtInf = null
            } else if (line.startsWith("http://") || line.startsWith("https://")) {
                // Standalone URL without preceding EXTINF
                val name = "Stream ${channels.size + 1}"
                channels.add(
                    Channel(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        streamUrl = line,
                        category = "Custom Stream",
                        resolution = detectResolution(name, line)
                    )
                )
            }
        }
        return channels
    }

    private fun parseChannelEntry(extInf: String, streamUrl: String): Channel? {
        if (streamUrl.isBlank()) return null

        val tvgId = TVG_ID_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
        val tvgName = TVG_NAME_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
        val tvgLogo = TVG_LOGO_REGEX.find(extInf)?.groupValues?.get(1) ?: ""
        val groupTitle = GROUP_TITLE_REGEX.find(extInf)?.groupValues?.get(1) ?: "General"

        // Channel display name is usually after the last comma
        val commaIndex = extInf.lastIndexOf(',')
        val displayName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
            extInf.substring(commaIndex + 1).trim()
        } else if (tvgName.isNotBlank()) {
            tvgName
        } else {
            "Channel ${extInf.hashCode().coerceAtLeast(1)}"
        }

        val resolution = detectResolution(displayName, streamUrl)
        val streamType = detectStreamType(groupTitle, displayName, streamUrl)

        return Channel(
            id = UUID.randomUUID().toString(),
            name = displayName,
            streamUrl = streamUrl,
            logoUrl = tvgLogo,
            category = groupTitle.ifBlank { "Entertainment" },
            groupTitle = groupTitle,
            tvgId = tvgId,
            resolution = resolution,
            isLive = streamType == StreamType.LIVE_TV,
            currentProgram = "Now Streaming",
            nextProgram = "Next Feature",
            description = "Channel: $displayName • Category: ${groupTitle.ifBlank { "Live TV" }}",
            streamType = streamType
        )
    }

    private fun detectResolution(name: String, url: String): String {
        val combined = "$name $url".uppercase()
        return when {
            combined.contains("4K") || combined.contains("UHD") || combined.contains("2160P") -> "4K UHD"
            combined.contains("1080P") || combined.contains("FHD") -> "1080p FHD"
            combined.contains("720P") || combined.contains("HD") -> "720p HD"
            else -> "1080p"
        }
    }

    private fun detectStreamType(group: String, name: String, url: String): StreamType {
        val combined = "$group $name $url".uppercase()
        return when {
            combined.contains("MOVIE") || combined.contains("CINEMA") || combined.contains("VOD") || url.endsWith(".mp4") -> StreamType.MOVIE
            combined.contains("SERIES") || combined.contains("SEASON") || combined.contains("EPISODE") -> StreamType.SERIES
            else -> StreamType.LIVE_TV
        }
    }
}
