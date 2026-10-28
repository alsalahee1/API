package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Channel
import com.example.ui.components.TvRemoteOverlay
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveChannelsScreen
import com.example.ui.screens.MultiStreamScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistManagerScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.Badge4K
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.IptvViewModel
import com.example.ui.viewmodel.ScreenDestination

class MainActivity : ComponentActivity() {
    private val viewModel: IptvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AuraIptvApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun AuraIptvApp(viewModel: IptvViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val channels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val allChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val aspectRatioMode by viewModel.aspectRatioMode.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val isAddingPlaylist by viewModel.isAddingPlaylist.collectAsStateWithLifecycle()
    val multiStreamSlots by viewModel.multiStreamSlots.collectAsStateWithLifecycle()
    val multiStreamCount by viewModel.multiStreamCount.collectAsStateWithLifecycle()
    val activeSlotIndex by viewModel.activeSlotIndex.collectAsStateWithLifecycle()
    val isTvMode by viewModel.isTvMode.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    // Sub-screen BackHandler navigation
    if (currentScreen != ScreenDestination.HOME && currentScreen != ScreenDestination.PLAYER) {
        BackHandler {
            viewModel.navigateTo(ScreenDestination.HOME)
        }
    }

    // Fullscreen video player has top precedence
    if (currentScreen == ScreenDestination.PLAYER && activeChannel != null) {
        PlayerScreen(
            channel = activeChannel!!,
            allChannels = channels,
            isFavorite = favoriteIds.contains(activeChannel!!.id),
            aspectRatioMode = aspectRatioMode,
            onToggleFavorite = { viewModel.toggleFavorite(activeChannel!!.id) },
            onCycleAspectRatio = { viewModel.cycleAspectRatio() },
            onSwitchChannel = { newChannel -> viewModel.playChannel(newChannel) },
            onOpenMultiStream = {
                viewModel.closePlayer()
                viewModel.navigateTo(ScreenDestination.MULTI_STREAM)
            },
            onClose = { viewModel.closePlayer() }
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val isWideScreen = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = DarkBackground,
            topBar = {
                AuraTopBar(
                    isTvMode = isTvMode,
                    onToggleTvMode = { viewModel.setTvMode(!isTvMode) },
                    onOpenMultiStream = { viewModel.navigateTo(ScreenDestination.MULTI_STREAM) },
                    onOpenPlaylists = { viewModel.navigateTo(ScreenDestination.PLAYLISTS) }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    AuraBottomNavBar(
                        currentScreen = currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Wide Screen Navigation Rail (Tablet / Desktop / TV)
                if (isWideScreen) {
                    AuraNavigationRail(
                        currentScreen = currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Optional TV remote hint banner
                    if (isTvMode) {
                        TvRemoteOverlay()
                    }

                    // Content Area
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentScreen) {
                            ScreenDestination.HOME -> {
                                HomeScreen(
                                    channels = channels,
                                    favoriteIds = favoriteIds,
                                    watchHistory = watchHistory,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onPlayChannel = { viewModel.playChannel(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onOpenMultiStream = { viewModel.navigateTo(ScreenDestination.MULTI_STREAM) }
                                )
                            }
                            ScreenDestination.LIVE_CHANNELS -> {
                                LiveChannelsScreen(
                                    channels = channels,
                                    favoriteIds = favoriteIds,
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    searchQuery = searchQuery,
                                    onSelectCategory = { viewModel.selectCategory(it) },
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onPlayChannel = { viewModel.playChannel(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                            ScreenDestination.MULTI_STREAM -> {
                                MultiStreamScreen(
                                    slots = multiStreamSlots,
                                    allChannels = channels,
                                    streamCount = multiStreamCount,
                                    activeSlotIndex = activeSlotIndex,
                                    onSelectSlot = { viewModel.selectMultiStreamSlot(it) },
                                    onChangeChannelForSlot = { slot, ch -> viewModel.setChannelForSlot(slot, ch) },
                                    onToggleSlotMute = { viewModel.toggleSlotMute(it) },
                                    onSetStreamCount = { viewModel.setMultiStreamCount(it) },
                                    onMaximizeStream = { viewModel.playChannel(it) }
                                )
                            }
                            ScreenDestination.PLAYLISTS -> {
                                PlaylistManagerScreen(
                                    playlists = playlists,
                                    isAdding = isAddingPlaylist,
                                    onAddUrl = { name, url -> viewModel.addPlaylistUrl(name, url) },
                                    onAddText = { name, text -> viewModel.addPlaylistText(name, text) },
                                    onAddXtream = { name, s, u, p -> viewModel.addXtreamPlaylist(name, s, u, p) },
                                    onDeletePlaylist = { viewModel.deletePlaylist(it) }
                                )
                            }
                            ScreenDestination.SEARCH -> {
                                SearchScreen(
                                    searchQuery = searchQuery,
                                    channels = channels,
                                    favoriteIds = favoriteIds,
                                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                    onPlayChannel = { viewModel.playChannel(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) }
                                )
                            }
                            ScreenDestination.PLAYER -> {
                                // Handled above
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraTopBar(
    isTvMode: Boolean,
    onToggleTvMode: () -> Unit,
    onOpenMultiStream: () -> Unit,
    onOpenPlaylists: () -> Unit
) {
    Surface(
        color = DarkSurface.copy(alpha = 0.95f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding (Apple TV aesthetic)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(NeonCyan, Color(0xFF9D4EDD))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "AURA",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = " TV",
                    color = NeonCyan,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.width(10.dp))

                Surface(
                    color = Badge4K,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "4K HDR",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Top action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // TV Mode switch button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTvMode) NeonCyan else DarkSurfaceVariant,
                    contentColor = if (isTvMode) Color.Black else Color.White,
                    modifier = Modifier
                        .clickable { onToggleTvMode() }
                        .testTag("tv_mode_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "TV Mode",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTvMode) "TV Remote: ON" else "TV Remote Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMultiStream,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("topbar_multistream_button")
                ) {
                    Icon(Icons.Default.GridView, contentDescription = "Multi-View", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }

                IconButton(
                    onClick = onOpenPlaylists,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("topbar_playlist_button")
                ) {
                    Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun AuraBottomNavBar(
    currentScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface.copy(alpha = 0.96f),
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .testTag("bottom_navigation_bar")
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = NeonCyan,
            indicatorColor = NeonCyan,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray
        )

        NavigationBarItem(
            selected = currentScreen == ScreenDestination.HOME,
            onClick = { onNavigate(ScreenDestination.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenDestination.LIVE_CHANNELS,
            onClick = { onNavigate(ScreenDestination.LIVE_CHANNELS) },
            icon = { Icon(Icons.Default.LiveTv, contentDescription = "Live Guide") },
            label = { Text("Live Guide", fontSize = 11.sp) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_live_guide")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenDestination.MULTI_STREAM,
            onClick = { onNavigate(ScreenDestination.MULTI_STREAM) },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Multi-View") },
            label = { Text("Multi-View", fontSize = 11.sp) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_multiview")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenDestination.PLAYLISTS,
            onClick = { onNavigate(ScreenDestination.PLAYLISTS) },
            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists") },
            label = { Text("Playlists", fontSize = 11.sp) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_playlists")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenDestination.SEARCH,
            onClick = { onNavigate(ScreenDestination.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search", fontSize = 11.sp) },
            colors = itemColors,
            modifier = Modifier.testTag("nav_search")
        )
    }
}

@Composable
fun AuraNavigationRail(
    currentScreen: ScreenDestination,
    onNavigate: (ScreenDestination) -> Unit
) {
    NavigationRail(
        containerColor = DarkSurface,
        header = {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = "Aura", tint = Color.Black)
            }
        },
        modifier = Modifier
            .fillMaxHeight()
            .testTag("navigation_rail")
    ) {
        val railColors = NavigationRailItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = NeonCyan,
            indicatorColor = NeonCyan,
            unselectedIconColor = Color.Gray,
            unselectedTextColor = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        NavigationRailItem(
            selected = currentScreen == ScreenDestination.HOME,
            onClick = { onNavigate(ScreenDestination.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = railColors
        )

        NavigationRailItem(
            selected = currentScreen == ScreenDestination.LIVE_CHANNELS,
            onClick = { onNavigate(ScreenDestination.LIVE_CHANNELS) },
            icon = { Icon(Icons.Default.LiveTv, contentDescription = "Live Guide") },
            label = { Text("Live", fontSize = 11.sp) },
            colors = railColors
        )

        NavigationRailItem(
            selected = currentScreen == ScreenDestination.MULTI_STREAM,
            onClick = { onNavigate(ScreenDestination.MULTI_STREAM) },
            icon = { Icon(Icons.Default.GridView, contentDescription = "Multi-View") },
            label = { Text("Multi", fontSize = 11.sp) },
            colors = railColors
        )

        NavigationRailItem(
            selected = currentScreen == ScreenDestination.PLAYLISTS,
            onClick = { onNavigate(ScreenDestination.PLAYLISTS) },
            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlists") },
            label = { Text("Playlists", fontSize = 11.sp) },
            colors = railColors
        )

        NavigationRailItem(
            selected = currentScreen == ScreenDestination.SEARCH,
            onClick = { onNavigate(ScreenDestination.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            label = { Text("Search", fontSize = 11.sp) },
            colors = railColors
        )
    }
}
