package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RecentLink
import com.example.ui.theme.immersiveGlowBackground
import com.example.ui.viewmodel.TeraViewModel
import com.example.ui.viewmodel.LinkHandler
import com.example.ui.viewmodel.NormalizationResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: TeraViewModel,
    onPasteAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val urlInput by viewModel.urlInput.collectAsState()
    val recentLinks by viewModel.recentLinks.collectAsState()
    val context = LocalContext.current

    val isPlausible = remember(urlInput) {
        urlInput.isBlank() || viewModel.isValidUrl(urlInput)
    }

    var showDeleteConfirmUrl by remember { mutableStateOf<String?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var showSoftFallbackDialog by remember { mutableStateOf<NormalizationResult?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .immersiveGlowBackground() // Immersive UI decorative space glows (violet + cyan)
    ) {
        // App Header: Designed according to top app bar in mock HTML
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp, start = 24.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFF06B6D4))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "TeraStream Logo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "TeraStream",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Scrape & Stream Any Link",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Simulated active context button from template mockup (h-10 w-10 active)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.05f), shape = CircleShape)
                    .clickable { /* decorative */ },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                )
            }
        }

        // Paste Section Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "PASTE LINK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B) // text-slate-500
                    ),
                    modifier = Modifier.padding(start = 4.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { viewModel.onUrlInputChange(it) },
                    placeholder = { Text("Paste terabox, nepopbox or direct stream URL...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_input_field"),
                    shape = RoundedCornerShape(16.dp), // exact rounded-2xl
                    singleLine = true,
                    isError = !isPlausible,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedContainerColor = Color(0xFF13141C),
                        unfocusedContainerColor = Color(0xFF1A1B23),
                        errorContainerColor = Color(0xFF1E1A1A),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onUrlInputChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipData = clipboard.primaryClip
                                    if (clipData != null && clipData.itemCount > 0) {
                                        val text = clipData.getItemAt(0).text?.toString() ?: ""
                                        viewModel.onUrlInputChange(text)
                                    }
                                },
                                modifier = Modifier.testTag("paste_clipboard_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste Clipboard",
                                    tint = Color(0xFF7C3AED) // violet-400
                                )
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = !isPlausible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = "Invalid link. Please include a supported TeraBox domain or direct video link.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Violet to Indigo Gradient Button: styled as "Stream Now" in mockup HTML
            Button(
                onClick = {
                    if (urlInput.isNotBlank() && isPlausible) {
                        val normalized = LinkHandler.normalizeAndExtractLink(urlInput)
                        if (normalized != null) {
                            if (LinkHandler.matchesLayer1(normalized.originalUrl) || LinkHandler.matchesLayer2(normalized.originalUrl)) {
                                viewModel.resolveLink(normalized.originalUrl)
                            } else {
                                showSoftFallbackDialog = normalized
                            }
                        }
                    }
                },
                enabled = urlInput.isNotBlank() && isPlausible,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("stream_submit_button")
                    .background(
                        if (urlInput.isNotBlank() && isPlausible) {
                            Brush.horizontalGradient(colors = listOf(Color(0xFF7C3AED), Color(0xFF4F46E5)))
                        } else {
                            Brush.horizontalGradient(colors = listOf(Color(0xFF7C3AED).copy(alpha = 0.3f), Color(0xFF4F46E5).copy(alpha = 0.3f)))
                        },
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                ),
                contentPadding = PaddingValues()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Stream Now", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Stream Icon",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Recents Row Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Recents",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            if (recentLinks.isNotEmpty()) {
                TextButton(
                    onClick = { showClearAllConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF7C3AED)), // text-violet-400
                    modifier = Modifier.testTag("clear_all_button"),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Clear all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Recent Links List Area
        if (recentLinks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Empty History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "History is clear",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Paste and resolve a link above to see play history.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentLinks, key = { it.url }) { recent ->
                    RecentLinkRow(
                        recent = recent,
                        onClick = {
                            viewModel.onUrlInputChange(recent.url)
                            viewModel.resolveLink(recent.url)
                        },
                        onLongClick = { showDeleteConfirmUrl = recent.url }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Decorative mock Navigation Bar matching Immersive UI html specification
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color(0xFF16171E))
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.05f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home tab - Active
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { /* Active view */ }
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7C3AED).copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Home",
                            tint = Color(0xFFC084FC),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Home",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC084FC)
                    )
                }

                // Inactive simulated tab - Saved
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { /* Decorative mockup action */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Saved",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                // Inactive simulated tab - Settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { /* Decorative mockup action */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Settings",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Settings",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }

    // Confirmation: Clear single link
    if (showDeleteConfirmUrl != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmUrl = null },
            title = { Text("Delete Entry?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("Are you sure you want to remove this link from records?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val url = showDeleteConfirmUrl
                        if (url != null) {
                            viewModel.deleteRecent(url)
                        }
                        showDeleteConfirmUrl = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmUrl = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Confirmation: Clear all links
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Clear All History?", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("This will permanently clear your entire search and stream history. This cannot be undone.", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllRecents()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Soft Fallback Dialog (Layer 3)
    if (showSoftFallbackDialog != null) {
        AlertDialog(
            onDismissRequest = { showSoftFallbackDialog = null },
            title = { Text("Unrecognized Link", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = { Text("This doesn't look like a TeraBox link. Try anyway?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val norm = showSoftFallbackDialog
                        if (norm != null) {
                            viewModel.resolveLink(norm.originalUrl)
                        }
                        showSoftFallbackDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Try anyway", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSoftFallbackDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentLinkRow(
    recent: RecentLink,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1B23)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("recent_item_${recent.title}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Rounded thumbnail box styling like mockup
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B)), // slate-800
                contentAlignment = Alignment.Center
            ) {
                if (!recent.thumbnail.isNullOrEmpty()) {
                    AsyncImage(
                        model = recent.thumbnail,
                        contentDescription = recent.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Text Info Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = recent.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFFF1F5F9) // slate-100
                )
                Text(
                    text = recent.url,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF64748B) // slate-500
                )
            }
        }
    }
}
