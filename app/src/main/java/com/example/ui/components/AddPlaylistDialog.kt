package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.M3UValidationResult
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveRed
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@Composable
fun AddPlaylistDialog(
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onValidateUrlSyntax: (String) -> M3UValidationResult,
    onValidateRemoteUrl: suspend (String) -> M3UValidationResult,
    onAddUrl: (String, String) -> Unit,
    onAddText: (String, String) -> Unit,
    onAddXtream: (String, String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var playlistName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var m3uText by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPass by remember { mutableStateOf("") }

    // Validation state
    var isValidatingRemote by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var syntaxErrorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Real-time client-side syntax validation
    LaunchedEffect(m3uUrl) {
        validationErrorMessage = null
        if (m3uUrl.isBlank()) {
            syntaxErrorMessage = null
        } else {
            val result = onValidateUrlSyntax(m3uUrl)
            syntaxErrorMessage = if (result is M3UValidationResult.Invalid) result.reason else null
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isValidatingRemote && !isAdding) onDismiss()
        },
        containerColor = DarkCardSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Add Playlist",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "M3U / M3U8 Stream Source with Validation",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            validationErrorMessage = null
                        },
                        text = { Text("M3U URL", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            validationErrorMessage = null
                        },
                        text = { Text("Paste Text", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            selectedTab = 2
                            validationErrorMessage = null
                        },
                        text = { Text("Xtream Codes", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Playlist Name (Optional)
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name (Optional)") },
                    placeholder = { Text("e.g. Cinema 4K & Sports", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // M3U URL Tab with real-time and remote validation
                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = {
                                m3uUrl = it
                            },
                            label = { Text("M3U / M3U8 Playlist URL") },
                            placeholder = { Text("https://example.com/stream_list.m3u", color = Color.Gray) },
                            singleLine = true,
                            isError = syntaxErrorMessage != null || validationErrorMessage != null,
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = "URL", tint = NeonCyan)
                            },
                            trailingIcon = {
                                if (m3uUrl.isNotEmpty()) {
                                    IconButton(onClick = {
                                        m3uUrl = ""
                                        validationErrorMessage = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                    }
                                }
                            },
                            supportingText = {
                                if (syntaxErrorMessage != null) {
                                    Text(syntaxErrorMessage!!, color = LiveRed, fontSize = 11.sp)
                                } else if (m3uUrl.isNotBlank()) {
                                    Text("Valid URL format (.m3u / .m3u8)", color = NeonCyan, fontSize = 11.sp)
                                } else {
                                    Text("Enter direct link to an M3U or M3U8 playlist file", color = Color.Gray, fontSize = 11.sp)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                errorBorderColor = LiveRed,
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                errorContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m3u_url_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick URL preset chips for instant testing
                        Text("Quick Test Presets:", color = Color.Gray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                onClick = {
                                    m3uUrl = "https://iptv-org.github.io/iptv/categories/news.m3u"
                                    if (playlistName.isBlank()) playlistName = "Global News"
                                }
                            ) {
                                Text(
                                    text = "News M3U",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                onClick = {
                                    m3uUrl = "https://iptv-org.github.io/iptv/languages/eng.m3u"
                                    if (playlistName.isBlank()) playlistName = "English Channels"
                                }
                            ) {
                                Text(
                                    text = "English M3U",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    1 -> {
                        // Raw M3U Paste
                        OutlinedTextField(
                            value = m3uText,
                            onValueChange = { m3uText = it },
                            label = { Text("Raw M3U Content") },
                            placeholder = { Text("#EXTM3U\n#EXTINF:-1,Channel 1\nhttps://...") },
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("m3u_text_input")
                        )
                    }

                    2 -> {
                        // Xtream Codes
                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Server URL (http://domain:port)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("Username") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = xtreamPass,
                            onValueChange = { xtreamPass = it },
                            label = { Text("Password") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Remote Validation Error Banner
                AnimatedVisibility(visible = validationErrorMessage != null) {
                    Surface(
                        color = LiveRed.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("validation_error_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = LiveRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationErrorMessage ?: "",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // In-progress Remote Validation Indicator
                AnimatedVisibility(visible = isValidatingRemote) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Validating M3U stream URL...",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            val canSubmit = when (selectedTab) {
                0 -> m3uUrl.isNotBlank() && syntaxErrorMessage == null
                1 -> m3uText.isNotBlank()
                2 -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank()
                else -> false
            }

            Button(
                onClick = {
                    if (selectedTab == 0) {
                        // Trigger validation logic before running M3U parser service
                        coroutineScope.launch {
                            isValidatingRemote = true
                            validationErrorMessage = null
                            val validationResult = onValidateRemoteUrl(m3uUrl)
                            isValidatingRemote = false

                            if (validationResult is M3UValidationResult.Invalid) {
                                validationErrorMessage = validationResult.reason
                            } else {
                                // Valid M3U file verified! Trigger M3U parser service
                                onAddUrl(playlistName, m3uUrl)
                            }
                        }
                    } else if (selectedTab == 1) {
                        onAddText(playlistName, m3uText)
                    } else if (selectedTab == 2) {
                        onAddXtream(playlistName, xtreamServer, xtreamUser, xtreamPass)
                    }
                },
                enabled = canSubmit && !isValidatingRemote && !isAdding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("validate_and_add_button")
            ) {
                if (isValidatingRemote || isAdding) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isValidatingRemote) "Validating..." else "Parsing...", fontWeight = FontWeight.Bold)
                } else {
                    Text(if (selectedTab == 0) "Validate & Add" else "Save & Parse", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isValidatingRemote && !isAdding
            ) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
