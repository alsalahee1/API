package com.example.data.service

import com.example.data.local.ChannelDao
import com.example.data.local.ChannelEntity
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Data layer service responsible for parsing M3U / M3U8 playlist files,
 * extracting channel names, logos, stream URLs, and metadata, and persisting
 * them into Room database entities for fast, reliable local storage.
 */
class M3UPlaylistService(
    private val channelDao: ChannelDao,
    private val playlistDao: PlaylistDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    private val TVG_ID_REGEX = """tvg-id="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val TVG_NAME_REGEX = """tvg-name="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val TVG_LOGO_REGEX = """tvg-logo="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
    private val GROUP_TITLE_REGEX = """group-title="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses an M3U playlist from a remote HTTP/HTTPS URL and stores the extracted
     * channels and playlist metadata into the local Room database.
     */
    suspend fun parseFromUrlAndStore(
        url: String,
        playlistId: String = UUID.randomUUID().toString(),
        playlistName: String = "Remote Playlist"
    ): Result<List<ChannelEntity>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Failed to download M3U from $url: HTTP ${response.code}")
                )
            }
            val body = response.body?.string().orEmpty()
            parseAndStore(
                m3uContent = body,
                playlistId = playlistId,
                playlistName = playlistName,
                sourceUrl = url,
                playlistType = "M3U_URL"
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses an M3U playlist from an [InputStream] (e.g. from local storage, file, or assets)
     * and persists the extracted channel entities into Room.
     */
    suspend fun parseFromStreamAndStore(
        inputStream: InputStream,
        playlistId: String = UUID.randomUUID().toString(),
        playlistName: String = "Stream Playlist",
        sourceUrl: String = "Local File"
    ): Result<List<ChannelEntity>> = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.use { it.readText() }
            parseAndStore(
                m3uContent = content,
                playlistId = playlistId,
                playlistName = playlistName,
                sourceUrl = sourceUrl,
                playlistType = "M3U_TEXT"
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses M3U playlist content directly from a String and saves all channel entities
     * into the local Room database in a single batch operation.
     */
    suspend fun parseAndStore(
        m3uContent: String,
        playlistId: String = UUID.randomUUID().toString(),
        playlistName: String = "Imported Playlist",
        sourceUrl: String = "Direct Input",
        playlistType: String = "M3U_TEXT"
    ): Result<List<ChannelEntity>> = withContext(Dispatchers.IO) {
        try {
            val entities = extractChannelEntities(m3uContent, playlistId)
            if (entities.isEmpty()) {
                return@withContext Result.failure(Exception("No valid channel streams found in M3U content"))
            }

            // Save playlist entity
            val playlistEntity = PlaylistEntity(
                id = playlistId,
                name = playlistName,
                sourceUrl = sourceUrl,
                type = playlistType,
                channelCount = entities.size,
                lastUpdated = System.currentTimeMillis(),
                isActive = true
            )
            playlistDao.insertPlaylist(playlistEntity)

            // Batch insert channel entities into Room
            channelDao.insertChannels(entities)

            Result.success(entities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Core extraction algorithm: parses M3U lines, extracting channel names, logos,
     * stream URLs, category, tvg-id, and resolution into a List of [ChannelEntity].
     */
    fun extractChannelEntities(
        m3uContent: String,
        playlistId: String = "default"
    ): List<ChannelEntity> {
        val channelList = mutableListOf<ChannelEntity>()
        val lines = m3uContent.lines()
        var currentExtInf: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = line
            } else if (!line.startsWith("#") && currentExtInf != null) {
                // Stream URL line immediately following #EXTINF line
                val entity = parseChannelLine(currentExtInf, line, playlistId)
                if (entity != null) {
                    channelList.add(entity)
                }
                currentExtInf = null
            } else if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtmp://")) {
                // Standalone stream URL without preceding #EXTINF header
                val generatedName = "Stream ${channelList.size + 1}"
                channelList.add(
                    ChannelEntity(
                        id = UUID.randomUUID().toString(),
                        name = generatedName,
                        streamUrl = line,
                        logoUrl = "",
                        category = "Custom Stream",
                        groupTitle = "Custom Stream",
                        resolution = detectResolution(generatedName, line),
                        playlistId = playlistId,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return channelList
    }

    private fun parseChannelLine(
        extInf: String,
        streamUrl: String,
        playlistId: String
    ): ChannelEntity? {
        if (streamUrl.isBlank()) return null

        val tvgId = TVG_ID_REGEX.find(extInf)?.groupValues?.get(1)?.trim() ?: ""
        val tvgName = TVG_NAME_REGEX.find(extInf)?.groupValues?.get(1)?.trim() ?: ""
        val tvgLogo = TVG_LOGO_REGEX.find(extInf)?.groupValues?.get(1)?.trim() ?: ""
        val groupTitle = GROUP_TITLE_REGEX.find(extInf)?.groupValues?.get(1)?.trim() ?: "General"

        // Extract channel display name: typically after the last comma in #EXTINF line
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

        return ChannelEntity(
            id = UUID.randomUUID().toString(),
            name = displayName,
            streamUrl = streamUrl,
            logoUrl = tvgLogo,
            category = groupTitle.ifBlank { "Entertainment" },
            groupTitle = groupTitle,
            tvgId = tvgId,
            resolution = resolution,
            isFavorite = false,
            isLive = streamType == "LIVE_TV",
            currentProgram = "Now Streaming",
            nextProgram = "Upcoming Program",
            description = "$displayName • Category: ${groupTitle.ifBlank { "General" }}",
            backdropUrl = tvgLogo,
            rating = "TV-14",
            streamType = streamType,
            playlistId = playlistId,
            addedAt = System.currentTimeMillis()
        )
    }

    private fun detectResolution(name: String, url: String): String {
        val combined = "$name $url".uppercase()
        return when {
            combined.contains("4K") || combined.contains("UHD") || combined.contains("2160P") -> "4K UHD"
            combined.contains("1080P") || combined.contains("FHD") -> "1080p FHD"
            combined.contains("720P") || combined.contains("HD") -> "720p HD"
            else -> "1080p FHD"
        }
    }

    private fun detectStreamType(group: String, name: String, url: String): String {
        val combined = "$group $name $url".uppercase()
        return when {
            combined.contains("MOVIE") || combined.contains("CINEMA") || combined.contains("VOD") || url.endsWith(".mp4") -> "MOVIE"
            combined.contains("SERIES") || combined.contains("SEASON") || combined.contains("EPISODE") -> "SERIES"
            else -> "LIVE_TV"
        }
    }
}
