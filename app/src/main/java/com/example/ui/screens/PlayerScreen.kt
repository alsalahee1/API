package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.ui.player.ExoPlayerView
import com.example.ui.theme.Badge4K
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.AspectRatioMode

@Composable
fun PlayerScreen(
    channel: Channel,
    allChannels: List<Channel>,
    isFavorite: Boolean,
    aspectRatioMode: AspectRatioMode,
    onToggleFavorite: () -> Unit,
    onCycleAspectRatio: () -> Unit,
    onSwitchChannel: (Channel) -> Unit,
    onOpenMultiStream: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChannelDrawerOpen by remember { mutableStateOf(false) }

    BackHandler {
        if (isChannelDrawerOpen) {
            isChannelDrawerOpen = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("full_player_screen")
    ) {
        // Main ExoPlayer View
        ExoPlayerView(
            channel = channel,
            aspectRatioMode = aspectRatioMode,
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onCycleAspectRatio = onCycleAspectRatio,
            onClose = onClose,
            onOpenMultiStream = onOpenMultiStream,
            modifier = Modifier.fillMaxSize()
        )

        // Floating Quick Channel Switcher Drawer button
        if (!isChannelDrawerOpen) {
            Surface(
                onClick = { isChannelDrawerOpen = true },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = DarkCardSurface.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .testTag("open_channel_drawer_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatListBulleted,
                        contentDescription = "Channels Guide",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Guide",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Side Channel Switcher Drawer (Quick channel surfing without stopping playback)
        AnimatedVisibility(
            visible = isChannelDrawerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp),
                color = DarkCardSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Quick Surf Guide",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${allChannels.size} streams available",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = { isChannelDrawerOpen = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allChannels, key = { it.id }) { ch ->
                            val isCurrent = ch.id == channel.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) NeonCyan.copy(alpha = 0.15f) else DarkSurfaceVariant,
                                border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, NeonCyan) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSwitchChannel(ch)
                                        isChannelDrawerOpen = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = ch.name,
                                                color = if (isCurrent) NeonCyan else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            if (ch.isLive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(LiveRed)
                                                )
                                            }
                                        }
                                        Text(
                                            text = ch.currentProgram,
                                            color = Color.LightGray,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }

                                    Surface(color = Badge4K, shape = RoundedCornerShape(3.dp)) {
                                        Text(
                                            text = ch.resolution,
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
