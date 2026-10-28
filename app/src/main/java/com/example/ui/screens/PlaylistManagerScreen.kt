package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FeaturedPlayList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.service.M3UValidationResult
import com.example.ui.components.AddPlaylistDialog

@Composable
fun PlaylistManagerScreen(
    playlists: List<Playlist>,
    isAdding: Boolean,
    onValidateUrlSyntax: (String) -> M3UValidationResult,
    onValidateRemoteUrl: suspend (String) -> M3UValidationResult,
    onAddUrl: (String, String) -> Unit,
    onAddText: (String, String) -> Unit,
    onAddXtream: (String, String, String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("playlist_manager_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Custom Playlist Manager",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Add M3U, M3U8 URLs, raw files, or Xtream Codes servers",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_playlist_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Playlist", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playlists List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistItemCard(
                        playlist = playlist,
                        onDelete = { onDeletePlaylist(playlist.id) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                    // Preset suggestions box
                    QuickPresetsBox(
                        onSelectPreset = { name, url ->
                            onAddUrl(name, url)
                        }
                    )
                }
            }
        }

        // Add Playlist Modal Dialog with Validation Logic
        if (showAddDialog) {
            AddPlaylistDialog(
                isAdding = isAdding,
                onDismiss = { showAddDialog = false },
                onValidateUrlSyntax = onValidateUrlSyntax,
                onValidateRemoteUrl = onValidateRemoteUrl,
                onAddUrl = { name, url ->
                    onAddUrl(name, url)
                    showAddDialog = false
                },
                onAddText = { name, text ->
                    onAddText(name, text)
                    showAddDialog = false
                },
                onAddXtream = { name, srv, u, p ->
                    onAddXtream(name, srv, u, p)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun PlaylistItemCard(
    playlist: Playlist,
    onDelete: () -> Unit
) {
    val dateStr = remember(playlist.lastUpdated) {
        SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(playlist.lastUpdated))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkCardSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (playlist.type) {
                            PlaylistType.M3U_URL -> Icons.Default.Link
                            PlaylistType.M3U_TEXT -> Icons.Default.Notes
                            PlaylistType.XTREAM_CODES -> Icons.Default.Storage
                            PlaylistType.CURATED_DEFAULT -> Icons.Default.FeaturedPlayList
                        },
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (playlist.type == PlaylistType.CURATED_DEFAULT) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NeonCyan.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL 4K",
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${playlist.channelCount} streams • $dateStr",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = playlist.sourceUrl,
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            if (playlist.type != PlaylistType.CURATED_DEFAULT) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_playlist_${playlist.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = LiveRed.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickPresetsBox(
    onSelectPreset: (String, String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Open-Source Presets", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Tap to load verified free public legal playlists from open broadcasting initiatives:",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSelectPreset(
                            "IPTV-org English Feeds",
                            "https://iptv-org.github.io/iptv/languages/eng.m3u"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text("English Channels M3U", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        onSelectPreset(
                            "Global News Streams",
                            "https://iptv-org.github.io/iptv/categories/news.m3u"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text("Global News M3U", fontSize = 11.sp, color = Color.White)
                }
            }
        }
    }
}

