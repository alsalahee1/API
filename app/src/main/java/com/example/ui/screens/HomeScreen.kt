package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FiberDvr
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.WatchHistoryItem
import com.example.ui.components.CategoryFilterTabs
import com.example.ui.components.ContentRow
import com.example.ui.components.HeroBillboard
import com.example.ui.theme.DarkBackground

@Composable
fun HomeScreen(
    channels: List<Channel>,
    favoriteIds: Set<String>,
    watchHistory: List<WatchHistoryItem>,
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onPlayChannel: (Channel) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenMultiStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val featuredChannel = remember(channels) {
        channels.firstOrNull { it.resolution.contains("4K") } ?: channels.firstOrNull()
    }

    // Filter categories for personalized carousels
    val continueWatchingChannels = remember(channels, watchHistory) {
        val historyMap = watchHistory.associateBy { it.channelId }
        channels.filter { historyMap.containsKey(it.id) }
    }
    val progressMap = remember(watchHistory) {
        watchHistory.associate { it.channelId to it.progressFraction }
    }

    val liveChannels = remember(channels) {
        channels.filter { it.isLive }
    }
    val cinemaChannels = remember(channels) {
        channels.filter { it.category.contains("Cinema", ignoreCase = true) || it.resolution.contains("4K") }
    }
    val sportsChannels = remember(channels) {
        channels.filter { it.category.contains("Sports", ignoreCase = true) }
    }
    val newsChannels = remember(channels) {
        channels.filter { it.category.contains("News", ignoreCase = true) }
    }
    val favoritesList = remember(channels, favoriteIds) {
        channels.filter { favoriteIds.contains(it.id) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("home_screen")
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Billboard Banner (Apple TV Aesthetic)
            if (featuredChannel != null) {
                item(key = "hero_billboard") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        HeroBillboard(
                            featuredChannel = featuredChannel,
                            isFavorite = favoriteIds.contains(featuredChannel.id),
                            onPlay = { onPlayChannel(featuredChannel) },
                            onToggleFavorite = { onToggleFavorite(featuredChannel.id) },
                            onOpenMultiStream = onOpenMultiStream
                        )
                    }
                }
            }

            // Quick Category Filters
            item(key = "category_filters") {
                CategoryFilterTabs(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelectCategory = onSelectCategory
                )
            }

            // Continue Watching Row (if any history)
            if (continueWatchingChannels.isNotEmpty()) {
                item(key = "continue_watching_row") {
                    ContentRow(
                        title = "Continue Watching",
                        icon = Icons.Default.History,
                        channels = continueWatchingChannels,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite,
                        watchProgressMap = progressMap
                    )
                }
            }

            // Favorites Row
            if (favoritesList.isNotEmpty()) {
                item(key = "favorites_row") {
                    ContentRow(
                        title = "My Watchlist & Favorites",
                        icon = Icons.Default.Bookmark,
                        channels = favoritesList,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            // Live On Air Now
            if (liveChannels.isNotEmpty()) {
                item(key = "live_channels_row") {
                    ContentRow(
                        title = "Live Broadcasts (On Air)",
                        icon = Icons.Default.Tv,
                        channels = liveChannels,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            // Cinema & 4K Masterpieces
            if (cinemaChannels.isNotEmpty()) {
                item(key = "cinema_channels_row") {
                    ContentRow(
                        title = "Cinema 4K Ultra HD",
                        icon = Icons.Default.Movie,
                        channels = cinemaChannels,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            // Sports & Live Events
            if (sportsChannels.isNotEmpty()) {
                item(key = "sports_channels_row") {
                    ContentRow(
                        title = "Sports & Live Action",
                        icon = Icons.Default.SportsScore,
                        channels = sportsChannels,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            // World News
            if (newsChannels.isNotEmpty()) {
                item(key = "news_channels_row") {
                    ContentRow(
                        title = "World News & Global Feeds",
                        icon = Icons.Default.Public,
                        channels = newsChannels,
                        favoriteIds = favoriteIds,
                        onChannelClick = onPlayChannel,
                        onToggleFavorite = onToggleFavorite
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
