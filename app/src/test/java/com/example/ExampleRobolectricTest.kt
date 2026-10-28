package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChannelDao
import com.example.data.local.PlaylistDao
import com.example.data.service.M3UPlaylistService
import com.example.data.service.M3UValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var channelDao: ChannelDao
    private lateinit var playlistDao: PlaylistDao
    private lateinit var m3uPlaylistService: M3UPlaylistService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        channelDao = database.channelDao()
        playlistDao = database.playlistDao()
        m3uPlaylistService = M3UPlaylistService(channelDao, playlistDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura IPTV", appName)
    }

    @Test
    fun `test M3U URL syntax validation`() {
        // Empty URL
        val emptyResult = m3uPlaylistService.validateUrlSyntax("")
        assertTrue(emptyResult is M3UValidationResult.Invalid)
        assertEquals("URL cannot be empty", (emptyResult as M3UValidationResult.Invalid).reason)

        // Missing scheme
        val noSchemeResult = m3uPlaylistService.validateUrlSyntax("example.com/playlist.m3u")
        assertTrue(noSchemeResult is M3UValidationResult.Invalid)
        assertEquals("URL must start with http:// or https://", (noSchemeResult as M3UValidationResult.Invalid).reason)

        // Valid HTTP and HTTPS URLs
        val validHttps = m3uPlaylistService.validateUrlSyntax("https://iptv-org.github.io/iptv/index.m3u")
        assertTrue(validHttps is M3UValidationResult.Valid)

        val validHttp = m3uPlaylistService.validateUrlSyntax("http://example.com:8080/live.m3u8")
        assertTrue(validHttp is M3UValidationResult.Valid)
    }

    @Test
    fun `test remote URL validation blocks invalid schemes before network`() = runBlocking {
        val invalidResult = m3uPlaylistService.validateRemoteM3UUrl("ftp://invalid.com/test.m3u")
        assertTrue(invalidResult is M3UValidationResult.Invalid)
        assertEquals("URL must start with http:// or https://", (invalidResult as M3UValidationResult.Invalid).reason)
    }

    @Test
    fun `test M3U playlist service extracts and stores channels in Room database`() = runBlocking {
        val m3uData = """
            #EXTM3U
            #EXTINF:-1 tvg-id="nasa.uhd" tvg-name="NASA UHD" tvg-logo="https://images.example.com/nasa.png" group-title="Science",NASA Television 4K
            https://streams.example.com/nasa/master.m3u8
            #EXTINF:-1 tvg-id="bloomberg.hd" tvg-logo="https://images.example.com/bloomberg.png" group-title="News",Bloomberg Global Live
            https://streams.example.com/bloomberg/live.m3u8
        """.trimIndent()

        val result = m3uPlaylistService.parseAndStore(
            m3uContent = m3uData,
            playlistId = "test_playlist_01",
            playlistName = "Test Space & News"
        )

        assertTrue(result.isSuccess)
        val channels = result.getOrNull()
        assertNotNull(channels)
        assertEquals(2, channels?.size)

        // Verify channels are stored in Room database
        val storedChannels = channelDao.getAllChannels().first()
        assertEquals(2, storedChannels.size)

        val channel1 = storedChannels.first { it.tvgId == "nasa.uhd" }
        assertEquals("NASA Television 4K", channel1.name)
        assertEquals("https://images.example.com/nasa.png", channel1.logoUrl)
        assertEquals("https://streams.example.com/nasa/master.m3u8", channel1.streamUrl)
        assertEquals("Science", channel1.category)
        assertEquals("4K UHD", channel1.resolution)
        assertEquals("test_playlist_01", channel1.playlistId)

        val channel2 = storedChannels.first { it.tvgId == "bloomberg.hd" }
        assertEquals("Bloomberg Global Live", channel2.name)
        assertEquals("https://images.example.com/bloomberg.png", channel2.logoUrl)
        assertEquals("https://streams.example.com/bloomberg/live.m3u8", channel2.streamUrl)
        assertEquals("News", channel2.category)

        // Verify playlist metadata is stored in Room
        val storedPlaylist = playlistDao.getPlaylistById("test_playlist_01")
        assertNotNull(storedPlaylist)
        assertEquals("Test Space & News", storedPlaylist?.name)
        assertEquals(2, storedPlaylist?.channelCount)
    }

    @Test
    fun `test favorite channel updates in Room database`() = runBlocking {
        val m3uData = """
            #EXTM3U
            #EXTINF:-1 tvg-id="redbull" tvg-logo="https://logo.com/rb.png",Red Bull Sports
            https://streams.example.com/rb.m3u8
        """.trimIndent()

        m3uPlaylistService.parseAndStore(m3uData, "pl_sports", "Sports")
        val channel = channelDao.getAllChannels().first().first()

        assertEquals(false, channel.isFavorite)

        // Toggle favorite to true in Room
        channelDao.updateFavorite(channel.id, true)
        val updatedChannel = channelDao.getChannelById(channel.id)
        assertNotNull(updatedChannel)
        assertEquals(true, updatedChannel?.isFavorite)

        val favorites = channelDao.getFavoriteChannels().first()
        assertEquals(1, favorites.size)
        assertEquals(channel.id, favorites[0].id)
    }
}
