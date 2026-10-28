package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.data.model.MultiStreamSlot
import com.example.ui.player.ExoPlayerView
import com.example.ui.theme.Badge4K
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiStreamScreen(
    slots: List<MultiStreamSlot>,
    allChannels: List<Channel>,
    streamCount: Int, // 2 or 4
    activeSlotIndex: Int,
    onSelectSlot: (Int) -> Unit,
    onChangeChannelForSlot: (Int, Channel) -> Unit,
    onToggleSlotMute: (Int) -> Unit,
    onSetStreamCount: (Int) -> Unit,
    onMaximizeStream: (Channel) -> Unit,
    modifier: Modifier = Modifier
) {
    var channelPickerSlot by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayedSlots = remember(slots, streamCount) {
        slots.take(streamCount)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("multi_stream_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Multi-Stream Arena",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            color = NeonCyan,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${streamCount}X LIVE",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tap or select slot to switch live audio • 4K UHD Ultra Streaming",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                // Grid layout switcher buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (streamCount == 2) NeonCyan else DarkSurfaceVariant,
                        contentColor = if (streamCount == 2) Color.Black else Color.White,
                        modifier = Modifier
                            .clickable { onSetStreamCount(2) }
                            .testTag("stream_count_2_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.ViewAgenda, contentDescription = "2 Streams", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("2 Views", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (streamCount == 4) NeonCyan else DarkSurfaceVariant,
                        contentColor = if (streamCount == 4) Color.Black else Color.White,
                        modifier = Modifier
                            .clickable { onSetStreamCount(4) }
                            .testTag("stream_count_4_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.GridOn, contentDescription = "4 Streams", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("4 Views", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Grid Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                if (streamCount == 2) {
                    // Split screen: 2 columns side by side or 2 rows
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayedSlots.forEach { slot ->
                            Box(modifier = Modifier.weight(1f)) {
                                MultiStreamCell(
                                    slot = slot,
                                    isActive = slot.slotIndex == activeSlotIndex,
                                    onSelect = { onSelectSlot(slot.slotIndex) },
                                    onChangeChannel = { channelPickerSlot = slot.slotIndex },
                                    onToggleMute = { onToggleSlotMute(slot.slotIndex) },
                                    onMaximize = { slot.channel?.let(onMaximizeStream) }
                                )
                            }
                        }
                    }
                } else {
                    // 4 streams: 2x2 grid
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val slot0 = displayedSlots.getOrNull(0)
                            val slot1 = displayedSlots.getOrNull(1)

                            Box(modifier = Modifier.weight(1f)) {
                                if (slot0 != null) {
                                    MultiStreamCell(
                                        slot = slot0,
                                        isActive = slot0.slotIndex == activeSlotIndex,
                                        onSelect = { onSelectSlot(slot0.slotIndex) },
                                        onChangeChannel = { channelPickerSlot = slot0.slotIndex },
                                        onToggleMute = { onToggleSlotMute(slot0.slotIndex) },
                                        onMaximize = { slot0.channel?.let(onMaximizeStream) }
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (slot1 != null) {
                                    MultiStreamCell(
                                        slot = slot1,
                                        isActive = slot1.slotIndex == activeSlotIndex,
                                        onSelect = { onSelectSlot(slot1.slotIndex) },
                                        onChangeChannel = { channelPickerSlot = slot1.slotIndex },
                                        onToggleMute = { onToggleSlotMute(slot1.slotIndex) },
                                        onMaximize = { slot1.channel?.let(onMaximizeStream) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val slot2 = displayedSlots.getOrNull(2)
                            val slot3 = displayedSlots.getOrNull(3)

                            Box(modifier = Modifier.weight(1f)) {
                                if (slot2 != null) {
                                    MultiStreamCell(
                                        slot = slot2,
                                        isActive = slot2.slotIndex == activeSlotIndex,
                                        onSelect = { onSelectSlot(slot2.slotIndex) },
                                        onChangeChannel = { channelPickerSlot = slot2.slotIndex },
                                        onToggleMute = { onToggleSlotMute(slot2.slotIndex) },
                                        onMaximize = { slot2.channel?.let(onMaximizeStream) }
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                if (slot3 != null) {
                                    MultiStreamCell(
                                        slot = slot3,
                                        isActive = slot3.slotIndex == activeSlotIndex,
                                        onSelect = { onSelectSlot(slot3.slotIndex) },
                                        onChangeChannel = { channelPickerSlot = slot3.slotIndex },
                                        onToggleMute = { onToggleSlotMute(slot3.slotIndex) },
                                        onMaximize = { slot3.channel?.let(onMaximizeStream) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Channel Selection Drawer for a specific Slot
        if (channelPickerSlot != null) {
            val slotIdx = channelPickerSlot!!
            ModalBottomSheet(
                onDismissRequest = { channelPickerSlot = null },
                sheetState = sheetState,
                containerColor = DarkCardSurface,
                dragHandle = null
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign Channel to Stream ${slotIdx + 1}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { channelPickerSlot = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) {
                        items(allChannels) { channel ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onChangeChannelForSlot(slotIdx, channel)
                                        channelPickerSlot = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(channel.currentProgram, color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Surface(color = Badge4K, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            channel.resolution,
                                            color = Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

@Composable
private fun MultiStreamCell(
    slot: MultiStreamSlot,
    isActive: Boolean,
    onSelect: () -> Unit,
    onChangeChannel: () -> Unit,
    onToggleMute: () -> Unit,
    onMaximize: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
            .focusable(interactionSource = interactionSource)
            .border(
                border = if (isActive || isFocused) BorderStroke(2.5.dp, NeonCyan) else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onSelect
            )
            .testTag("multistream_cell_${slot.slotIndex}")
    ) {
        val channel = slot.channel
        if (channel != null) {
            // Live Video feed inside cell
            ExoPlayerView(
                channel = channel,
                isMuted = slot.isMuted,
                showControlsOverlay = false,
                modifier = Modifier.fillMaxSize()
            )

            // Cell HUD overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Channel Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${slot.slotIndex + 1}. ${channel.name}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isActive) {
                            Surface(color = NeonCyan, shape = RoundedCornerShape(3.dp)) {
                                Text(
                                    text = "AUDIO ON",
                                    color = Color.Black,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Cell Actions: Mute / Swap / Maximize
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleMute,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (slot.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Mute",
                            tint = if (slot.isMuted) Color.Gray else NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onChangeChannel,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap Channel",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onMaximize,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Full Screen",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        } else {
            // Empty Slot placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onChangeChannel() },
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Channel",
                    tint = NeonCyan,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Assign Stream ${slot.slotIndex + 1}",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
