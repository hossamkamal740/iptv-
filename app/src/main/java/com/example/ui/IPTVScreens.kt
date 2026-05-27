package com.example.ui

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChannelEntity
import com.example.data.PlaylistEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen Enumeration
enum class Screen {
    Splash,
    Setup,
    Dashboard,
    Player
}

// Global Localization helper that resolves strings dynamically based on Selected Language (Arabic / English)
@Composable
fun appString(enResId: Int, arResId: Int? = null): String {
    // If the locale resources are updated, Android OS does this automatically.
    // We also read standard values from resources.
    return stringResource(enResId)
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(onNavigateToNext: () -> Unit) {
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
    val alpha = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(key1 = true) {
        // Parallel animation
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        // Splash delay
        delay(2500)
        onNavigateToNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DarkCrimson, BackgroundDark),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated Premium Logo Sphere
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(PremiumRed.copy(alpha = 0.15f))
                    .border(2.dp, PremiumRed, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "App Logo",
                    tint = PremiumRed,
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = stringResource(R.string.app_desc),
                fontSize = 14.sp,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = PremiumRed,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

// ==========================================
// 2. SETUP / LOGIN SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    viewModel: IPTVViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var playlistType by remember { mutableStateOf("m3u") } // "m3u" or "xtream"
    
    // Form Inputs
    var nameInput by remember { mutableStateOf("") }
    var m3uUrlInput by remember { mutableStateOf("") }
    var hostInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Screen Dimensions for Responsive Scaling
    val configuration = LocalConfiguration.current
    val isTabletOrLandscape = configuration.screenWidthDp > 600

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // SIDEBAR FOR LARGE DEVICES: Displays Saved Playlists
            if (isTabletOrLandscape && playlists.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(),
                    color = CardSurfaceDark,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.set_playlist),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 12.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(playlists) { playlist ->
                                PlaylistItemCard(
                                    playlist = playlist,
                                    isActive = playlist.isActive,
                                    onSelect = {
                                        viewModel.switchPlaylist(playlist.id)
                                        onNavigateToDashboard()
                                    },
                                    onDelete = {
                                        viewModel.deletePlaylist(playlist)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // MAIN REGISTRATION FORM
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = stringResource(R.string.setup_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.setup_subtitle),
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Auto Setup Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f)
                        .padding(bottom = 20.dp)
                        .border(1.dp, PremiumRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Auto",
                                tint = PremiumRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.auto_setup_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.auto_setup_desc),
                            fontSize = 12.sp,
                            color = TextSecondaryDark,
                            lineHeight = 16.sp,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Arabic Channels Easy Button
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    val arabicName = "القنوات المفتوحة (عربي)"
                                    val arabicUrl = "https://iptv-org.github.io/iptv/languages/ara.m3u"
                                    viewModel.addPlaylist(
                                        name = arabicName,
                                        type = "m3u",
                                        m3uUrl = arabicUrl,
                                        onSuccess = {
                                            onNavigateToDashboard()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.btn_load_arabic_channels),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            // 2. Global News Easy Button
                            Button(
                                onClick = {
                                    viewModel.clearError()
                                    val newsName = "قنوات الأخبار الدولية"
                                    val newsUrl = "https://iptv-org.github.io/iptv/categories/news.m3u"
                                    viewModel.addPlaylist(
                                        name = newsName,
                                        type = "m3u",
                                        m3uUrl = newsUrl,
                                        onSuccess = {
                                            onNavigateToDashboard()
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.btn_load_global_news),
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // OR divider separator (aesthetic pairing)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f)
                        .padding(bottom = 20.dp)
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.07f))
                    Text(
                        text = "أو الإدخال اليدوي / OR MANUAL SETUP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.07f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Tab Selector
                TabRow(
                    selectedTabIndex = if (playlistType == "m3u") 0 else 1,
                    containerColor = CardSurfaceDark,
                    contentColor = PremiumRed,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            color = PremiumRed,
                            modifier = Modifier.tabIndicatorOffset(tabPositions[if (playlistType == "m3u") 0 else 1])
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = playlistType == "m3u",
                        onClick = { playlistType = "m3u"; viewModel.clearError() },
                        text = { Text(stringResource(R.string.playlist_type_m3u), fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = playlistType == "xtream",
                        onClick = { playlistType = "xtream"; viewModel.clearError() },
                        text = { Text(stringResource(R.string.playlist_type_xtream), fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Errors Overlay
                if (errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Inputs Group
                Column(
                    modifier = Modifier.fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Playlist Name
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.field_playlist_name)) },
                        colors = transparentTextFieldColors(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = PremiumRed) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (playlistType == "m3u") {
                        // 2. M3U Link URL
                        OutlinedTextField(
                            value = m3uUrlInput,
                            onValueChange = { m3uUrlInput = it },
                            label = { Text(stringResource(R.string.field_m3u_url)) },
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = PremiumRed) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            placeholder = { Text("https://example.com/playlist.m3u") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // 3. Xtream Server Link
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text(stringResource(R.string.field_host_url)) },
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, tint = PremiumRed) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            placeholder = { Text("http://provider.tv:8080") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4. Xtream Username
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(stringResource(R.string.field_username)) },
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PremiumRed) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 5. Xtream Password
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text(stringResource(R.string.field_password)) },
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PremiumRed) },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Call Buttons
                Button(
                    onClick = {
                        viewModel.clearError()
                        if (nameInput.isEmpty() || 
                            (playlistType == "m3u" && m3uUrlInput.isEmpty()) ||
                            (playlistType == "xtream" && (hostInput.isEmpty() || usernameInput.isEmpty() || passwordInput.isEmpty()))
                        ) {
                            coroutineScope.launch {
                                // Simple trigger error
                            }
                            return@Button
                        }
                        
                        viewModel.addPlaylist(
                            name = nameInput,
                            type = playlistType,
                            m3uUrl = m3uUrlInput,
                            host = hostInput,
                            username = usernameInput,
                            password = passwordInput,
                            onSuccess = {
                                onNavigateToDashboard()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth(if (isTabletOrLandscape) 0.6f else 1f)
                        .height(52.dp)
                        .testTag("submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.btn_connect),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Show playlists at bottom for phone devices
                if (!isTabletOrLandscape && playlists.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(36.dp))
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = stringResource(R.string.set_playlist),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        playlists.forEach { playlist ->
                            PlaylistItemCard(
                                playlist = playlist,
                                isActive = playlist.isActive,
                                onSelect = {
                                    viewModel.switchPlaylist(playlist.id)
                                    onNavigateToDashboard()
                                },
                                onDelete = {
                                    viewModel.deletePlaylist(playlist)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Fullscreen Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark.copy(alpha = 0.85f))
                    .clickable(enabled = false) {}, // Absorb click gestures
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .widthIn(max = 400.dp)
                        .border(1.dp, PremiumRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = PremiumRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.auto_loading),
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (configuration.locales[0].language == "ar") 
                                "جاري تهيئة القنوات المفتوحة وترتيب المصادر الرقمية الفورية لمشاهدة آمنة وقانونية."
                                else "Configuring open channels and preparing real-time digital streams.",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistItemCard(
    playlist: PlaylistEntity,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) PremiumRed.copy(alpha = 0.15f) else CardSurfaceDark
        ),
        border = BorderStroke(
            1.5.dp, 
            if (isActive) PremiumRed else Color.White.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (playlist.type == "m3u") Icons.Default.FeaturedPlayList else Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = if (isActive) PremiumRed else TextSecondaryDark,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (playlist.type == "m3u") "M3U Link" else "Xtream Codes API",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Playlist",
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 3. MAIN DASHBOARD SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainDashboard(
    viewModel: IPTVViewModel,
    onNavigateToPlayer: (ChannelEntity) -> Unit,
    onNavigateToSetup: () -> Unit
) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var activeTab by remember { mutableStateOf("home") } // "home", "all", "favs", "history", "settings"

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTablet = screenWidth > 600

    // Dynamic grid columns based on screen width
    val gridColumns = when {
        screenWidth > 900 -> 5
        screenWidth > 600 -> 3
        else -> 2
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) BackgroundDark else BackgroundLight)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // TOP BAR: App Icon, Brand Name, Search & Language Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PremiumRed)
                        .clickable { onNavigateToSetup() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePlaylist?.name ?: stringResource(R.string.app_name),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else TextPrimaryLight,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (activePlaylist != null) stringResource(R.string.player_live) else stringResource(R.string.app_desc),
                        color = PremiumRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Language Toggler (English / Arabic RTL testing support)
                IconButton(
                    onClick = {
                        onNavigateToSetup()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlaylistAdd,
                        contentDescription = "Manage Playlists",
                        tint = if (isDarkMode) Color.White else TextPrimaryLight
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleDarkMode() }
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isDarkMode) Color.White else TextPrimaryLight
                    )
                }
            }

            // SEARCH BAR COMPONENT
            if (activeTab != "settings") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    colors = searchTextFieldColors(isDarkMode),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PremiumRed) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.5f))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MAIN CONTENT ROW (Adaptive sidebar layout on tablets / bottom tabs on phones)
            Row(modifier = Modifier.weight(1f)) {
                // Side Navigation on large screens
                if (isTablet) {
                    NavigationRail(
                        containerColor = Color.Transparent,
                        contentColor = PremiumRed,
                        modifier = Modifier.width(84.dp)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        NavRailItem(
                            selected = activeTab == "home",
                            onClick = { activeTab = "home"; viewModel.setSearchQuery("") },
                            icon = Icons.Default.Home,
                            label = stringResource(R.string.nav_home),
                            isDarkMode = isDarkMode
                        )
                        NavRailItem(
                            selected = activeTab == "all",
                            onClick = { activeTab = "all"; viewModel.setSearchQuery("") },
                            icon = Icons.Default.List,
                            label = stringResource(R.string.nav_categories),
                            isDarkMode = isDarkMode
                        )
                        NavRailItem(
                            selected = activeTab == "favs",
                            onClick = { activeTab = "favs"; viewModel.setSearchQuery("") },
                            icon = Icons.Default.Favorite,
                            label = stringResource(R.string.nav_favorites),
                            isDarkMode = isDarkMode
                        )
                        NavRailItem(
                            selected = activeTab == "history",
                            onClick = { activeTab = "history"; viewModel.setSearchQuery("") },
                            icon = Icons.Default.History,
                            label = stringResource(R.string.nav_recent),
                            isDarkMode = isDarkMode
                        )
                        NavRailItem(
                            selected = activeTab == "settings",
                            onClick = { activeTab = "settings" },
                            icon = Icons.Default.Settings,
                            label = stringResource(R.string.nav_settings),
                            isDarkMode = isDarkMode
                        )
                    }
                    Divider(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                    )
                }

                // Dynamic Screen switching centered view
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PremiumRed)
                        }
                    } else if (activePlaylist == null) {
                        // EMPTY PLAYLIST FALLBACK CONTAINER
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = PremiumRed,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = stringResource(R.string.setup_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else TextPrimaryLight
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.setup_subtitle),
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) TextSecondaryDark else TextSecondaryLight,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToSetup,
                                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                                ) {
                                    Text(stringResource(R.string.playlist_type_m3u))
                                }
                            }
                        }
                    } else {
                        when (activeTab) {
                            "home" -> HomeViewTab(
                                viewModel = viewModel,
                                history = history,
                                favorites = favorites,
                                channels = channels,
                                isDarkMode = isDarkMode,
                                groups = groups,
                                onChannelSelected = onNavigateToPlayer,
                                onSelectGroupTab = {
                                    viewModel.selectGroup(it)
                                    activeTab = "all"
                                }
                            )
                            "all" -> CategoriesViewTab(
                                viewModel = viewModel,
                                channels = channels,
                                groups = groups,
                                selectedGroup = selectedGroup,
                                isDarkMode = isDarkMode,
                                gridColumns = gridColumns,
                                onChannelSelected = onNavigateToPlayer
                            )
                            "favs" -> FavoritesViewTab(
                                favorites = favorites,
                                isDarkMode = isDarkMode,
                                gridColumns = gridColumns,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelSelected = onNavigateToPlayer
                            )
                            "history" -> RecentsHistoryViewTab(
                                history = history,
                                favorites = favorites,
                                isDarkMode = isDarkMode,
                                gridColumns = gridColumns,
                                onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                onChannelSelected = onNavigateToPlayer,
                                onClearHistory = { viewModel.clearHistory() }
                            )
                            "settings" -> SettingsViewTab(
                                viewModel = viewModel,
                                activePlaylist = activePlaylist!!,
                                isDarkMode = isDarkMode,
                                onManagePlaylists = onNavigateToSetup
                            )
                        }
                    }
                }
            }

            // Bottom Navigation on Portable mobile layouts
            if (!isTablet) {
                Divider(color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
                NavigationBar(
                    containerColor = if (isDarkMode) CardSurfaceDark else Color.White,
                    contentColor = PremiumRed,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BottomNavItem(
                        selected = activeTab == "home",
                        onClick = { activeTab = "home"; viewModel.setSearchQuery("") },
                        icon = Icons.Default.Home,
                        label = stringResource(R.string.nav_home),
                        isDarkMode = isDarkMode
                    )
                    BottomNavItem(
                        selected = activeTab == "all",
                        onClick = { activeTab = "all"; viewModel.setSearchQuery("") },
                        icon = Icons.Default.FeaturedPlayList,
                        label = stringResource(R.string.nav_categories),
                        isDarkMode = isDarkMode
                    )
                    BottomNavItem(
                        selected = activeTab == "favs",
                        onClick = { activeTab = "favs"; viewModel.setSearchQuery("") },
                        icon = Icons.Default.Favorite,
                        label = stringResource(R.string.nav_favorites),
                        isDarkMode = isDarkMode
                    )
                    BottomNavItem(
                        selected = activeTab == "history",
                        onClick = { activeTab = "history"; viewModel.setSearchQuery("") },
                        icon = Icons.Default.History,
                        label = stringResource(R.string.nav_recent),
                        isDarkMode = isDarkMode
                    )
                    BottomNavItem(
                        selected = activeTab == "settings",
                        onClick = { activeTab = "settings" },
                        icon = Icons.Default.Settings,
                        label = stringResource(R.string.nav_settings),
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. HOME TAB - CONTENT
// ==========================================
@Composable
fun HomeViewTab(
    viewModel: IPTVViewModel,
    history: List<ChannelEntity>,
    favorites: List<ChannelEntity>,
    channels: List<ChannelEntity>,
    isDarkMode: Boolean,
    groups: List<String>,
    onChannelSelected: (ChannelEntity) -> Unit,
    onSelectGroupTab: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        
        // Dynamic Netflix-Style banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PremiumRed)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(PremiumRed, DarkCrimson, Color.Transparent)
                            )
                        )
                    }
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.welcome),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.welcome_sub),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        // HISTORY CORNER
        if (history.isNotEmpty()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nav_recent),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else TextPrimaryLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(history) { channel ->
                        ChannelCircularIconItem(channel, isDarkMode) {
                            onChannelSelected(channel)
                        }
                    }
                }
            }
        }

        // INTERESTING CATEGORIES ROUNDUP
        Column {
            Text(
                text = stringResource(R.string.nav_categories),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pick top 8 groups for dashboard preview
                items(groups.take(8)) { group ->
                    FilterChip(
                        selected = false,
                        onClick = { onSelectGroupTab(group) },
                        label = { Text(text = group, color = Color.White, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isDarkMode) CardSurfaceDark else Color.White.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("filter_chip_$group")
                    )
                }
            }
        }

        // POPULAR LIVE FLOWS CAROUSEL
        Column {
            Text(
                text = stringResource(R.string.category_all),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.White else TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            if (channels.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_channels_title),
                    color = TextSecondaryDark,
                    fontSize = 13.sp
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(channels.take(15)) { channel ->
                        ChannelPreviewCard(channel, isDarkMode) {
                            onChannelSelected(channel)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. CATEGORIES VIEW
// ==========================================
@Composable
fun CategoriesViewTab(
    viewModel: IPTVViewModel,
    channels: List<ChannelEntity>,
    groups: List<String>,
    selectedGroup: String,
    isDarkMode: Boolean,
    gridColumns: Int,
    onChannelSelected: (ChannelEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Group chips horizontal scroll
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(groups) { group ->
                val isSelected = group == selectedGroup
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectGroup(group) },
                    label = { Text(group, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PremiumRed,
                        selectedLabelColor = Color.White,
                        containerColor = if (isDarkMode) CardSurfaceDark else Color(0xFFE5E5EA),
                        labelColor = if (isDarkMode) Color.White else TextPrimaryLight
                    ),
                    modifier = Modifier.testTag("category_chip_$group")
                )
            }
        }

        if (channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_channels_title),
                    color = TextSecondaryDark,
                    fontSize = 14.sp
                )
            }
        } else {
            val favorites by viewModel.favorites.collectAsState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
                    val isFavorite = remember(favorites, channel.id) { favorites.any { it.id == channel.id } }
                    ChannelInteractiveGridCard(
                        channel = channel,
                        isDarkMode = isDarkMode,
                        isFavorite = isFavorite,
                        onFavoriteToggle = { viewModel.toggleFavorite(channel) }
                    ) {
                        onChannelSelected(channel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. FAVORITES VIEW
// ==========================================
@Composable
fun FavoritesViewTab(
    favorites: List<ChannelEntity>,
    isDarkMode: Boolean,
    gridColumns: Int,
    onFavoriteToggle: (ChannelEntity) -> Unit,
    onChannelSelected: (ChannelEntity) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = PremiumRed,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_favorites_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.no_favorites_desc),
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favorites, key = { it.id }) { channel ->
                ChannelInteractiveGridCard(
                    channel = channel,
                    isDarkMode = isDarkMode,
                    isFavorite = true,
                    onFavoriteToggle = { onFavoriteToggle(channel) }
                ) {
                    onChannelSelected(channel)
                }
            }
        }
    }
}

// ==========================================
// 7. RECENTS WATCHED VIEW
// ==========================================
@Composable
fun RecentsHistoryViewTab(
    history: List<ChannelEntity>,
    favorites: List<ChannelEntity>,
    isDarkMode: Boolean,
    gridColumns: Int,
    onFavoriteToggle: (ChannelEntity) -> Unit,
    onChannelSelected: (ChannelEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.HistoryToggleOff,
                    contentDescription = null,
                    tint = PremiumRed,
                    modifier = Modifier.size(68.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_recent_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.no_recent_desc),
                    fontSize = 13.sp,
                    color = TextSecondaryDark,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearHistory) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = PremiumRed)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = PremiumRed)
                }
            }
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(history, key = { it.id }) { channel ->
                    val isFavorite = remember(favorites, channel.id) { favorites.any { it.id == channel.id } }
                    ChannelInteractiveGridCard(
                        channel = channel,
                        isDarkMode = isDarkMode,
                        isFavorite = isFavorite,
                        onFavoriteToggle = { onFavoriteToggle(channel) }
                    ) {
                        onChannelSelected(channel)
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. SETTINGS VIEW
// ==========================================
@Composable
fun SettingsViewTab(
    viewModel: IPTVViewModel,
    activePlaylist: PlaylistEntity,
    isDarkMode: Boolean,
    onManagePlaylists: () -> Unit
) {
    var appLanguage by remember { mutableStateOf("ar") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else TextPrimaryLight,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Custom Preference items
        ListItemPreference(
            title = stringResource(R.string.set_theme),
            desc = stringResource(R.string.set_dark_mode),
            isDarkMode = isDarkMode,
            trailing = {
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.toggleDarkMode() },
                    colors = SwitchDefaults.colors(checkedThumbColor = PremiumRed, checkedTrackColor = PremiumRed.copy(alpha = 0.5f))
                )
            }
        )

        ListItemPreference(
            title = stringResource(R.string.set_playlist),
            desc = "Working profile: ${activePlaylist.name}",
            isDarkMode = isDarkMode,
            trailing = {
                Button(
                    onClick = onManagePlaylists,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Configure", fontSize = 12.sp)
                }
            }
        )

        ListItemPreference(
            title = stringResource(R.string.set_clear_cache),
            desc = stringResource(R.string.set_clear_cache_desc),
            isDarkMode = isDarkMode,
            trailing = {
                OutlinedButton(
                    onClick = { viewModel.clearAppCache() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PremiumRed),
                    border = BorderStroke(1.dp, PremiumRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Truncate")
                }
            }
        )

        // Parental controls preferences
        val isParentalLockActive by viewModel.isParentalLockActive.collectAsState()
        val parentalPinCode by viewModel.parentalPin.collectAsState()
        var showPinDialog by remember { mutableStateOf(false) }
        var pinInput by remember { mutableStateOf("") }
        var isPinError by remember { mutableStateOf(false) }

        var showChangePinDialog by remember { mutableStateOf(false) }
        var oldPinInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var pinChangeError by remember { mutableStateOf(false) }

        val localeLanguage = remember { java.util.Locale.getDefault().language }

        ListItemPreference(
            title = if (localeLanguage == "ar") "حماية الرقابة الأبوية (الوضع الآمن)" else "Parental Control (Safe Mode)",
            desc = if (localeLanguage == "ar") "تصفية وحجب قنوات البالغين وقنوات +18 تلقائياً" else "Automatically filter adult and sensitive +18 content",
            isDarkMode = isDarkMode,
            trailing = {
                Switch(
                    checked = isParentalLockActive,
                    onCheckedChange = { active ->
                        showPinDialog = true
                        pinInput = ""
                        isPinError = false
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = PremiumRed, checkedTrackColor = PremiumRed.copy(alpha = 0.5f))
                )
            }
        )

        if (isParentalLockActive) {
            ListItemPreference(
                title = if (localeLanguage == "ar") "تعديل رمز الأمان (PIN)" else "Change Parental Action PIN",
                desc = if (localeLanguage == "ar") "تحديث رمز PIN لتخطي تصفية وحجب القنوات" else "Update the code used to toggle parental lock filters",
                isDarkMode = isDarkMode,
                trailing = {
                    Button(
                        onClick = {
                            showChangePinDialog = true
                            oldPinInput = ""
                            newPinInput = ""
                            pinChangeError = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumRed),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (localeLanguage == "ar") "تعديل" else "Modify", fontSize = 11.sp, color = Color.White)
                    }
                }
            )
        }

        // Beautiful PIN verification dialog
        if (showPinDialog) {
            AlertDialog(
                onDismissRequest = { showPinDialog = false },
                containerColor = CardSurfaceDark,
                modifier = Modifier.border(1.dp, PremiumRed.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                title = {
                    Text(
                        text = if (localeLanguage == "ar") "رمز الأمان للرقابة الأبوية" else "Parental Authentication",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (localeLanguage == "ar") "أدخل الرمز السري المكون من 4 أرقام لتفعيل/إلغاء القفل (الافتراضي: 1111)" else "Please enter your 4-digit PIN (Default is 1111)",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                            placeholder = { Text("PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            isError = isPinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isPinError) {
                            Text(
                                text = if (localeLanguage == "ar") "الرمز السري غير صحيح، يرجى المحاولة مجدداً!" else "Incorrect PIN! Please retry.",
                                color = PremiumRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pinInput == parentalPinCode) {
                                viewModel.setParentalLock(!isParentalLockActive)
                                showPinDialog = false
                            } else {
                                isPinError = true
                            }
                        }
                    ) {
                        Text(if (localeLanguage == "ar") "تأكيد والتحقق" else "Verify & OK", color = PremiumRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPinDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Change Pin Dialog
        if (showChangePinDialog) {
            AlertDialog(
                onDismissRequest = { showChangePinDialog = false },
                containerColor = CardSurfaceDark,
                modifier = Modifier.border(1.dp, PremiumRed.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                title = {
                    Text(
                        text = if (localeLanguage == "ar") "تعديل الرمز السري" else "Change Lock PIN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (localeLanguage == "ar") "املأ الحقول التالية لتحديث رمز الرقابة الأبوية" else "Provide the values to update code",
                            color = TextSecondaryDark,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = oldPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) oldPinInput = it },
                            label = { Text(if (localeLanguage == "ar") "رمز الأمان الحالي" else "Current PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPinInput = it },
                            label = { Text(if (localeLanguage == "ar") "الرمز الجديد (4 أرقام)" else "New PIN (4 Digits)") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = transparentTextFieldColors(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (pinChangeError) {
                            Text(
                                text = if (localeLanguage == "ar") "تأكد من إدخال رمز الأمان القديم الصحيح ورمّز جديّد مكون من 4 أرقام" else "Verify inputs (old must match, new must be 4 digits)",
                                color = PremiumRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (oldPinInput == parentalPinCode && newPinInput.length == 4 && newPinInput.all { it.isDigit() }) {
                                viewModel.setParentalPin(newPinInput)
                                showChangePinDialog = false
                            } else {
                                pinChangeError = true
                            }
                        }
                    ) {
                        Text(if (localeLanguage == "ar") "حفظ الرمز السري الجديد" else "Save New PIN", color = PremiumRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChangePinDialog = false }) {
                        Text(stringResource(R.string.btn_cancel), color = Color.White.copy(alpha = 0.6f))
                    }
                }
            )
        }

        // Information About card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) CardSurfaceDark else Color.White
            ),
            border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.info_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.info_desc),
                    fontSize = 12.sp,
                    color = if (isDarkMode) TextSecondaryDark else TextSecondaryLight,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ListItemPreference(
    title: String,
    desc: String,
    isDarkMode: Boolean,
    trailing: @Composable () -> Unit
) {
    Surface(
        color = if (isDarkMode) CardSurfaceDark else Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else TextPrimaryLight, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, color = if (isDarkMode) TextSecondaryDark else TextSecondaryLight, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

// ==========================================
// 9. PROFESSIONAL EXOPLAYER SCREEN
// ==========================================
// Data structures for Video Quality Selection
data class QualityPreset(
    val label: String,
    val maxWidth: Int,
    val maxHeight: Int,
    val description: String
)

data class DetectedTrack(
    val group: Tracks.Group,
    val index: Int,
    val label: String,
    val bitrate: Int,
    val isSelected: Boolean
)

// ==========================================
@Composable
fun PlayerScreen(
    viewModel: IPTVViewModel,
    channel: ChannelEntity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allChannels by viewModel.channels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    // Media3 ExoPlayer setup declared early to support state handlers
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Local channel state to allow in-player quick channel switching!
    var currentPlayingChannel by remember(channel) { mutableStateOf(channel) }
    var showQuickChannelsDrawer by remember { mutableStateOf(false) }

    // Filter available channels belonging to the active category
    val groupChannels = remember(allChannels, currentPlayingChannel.groupTitle) {
        allChannels.filter { it.groupTitle == currentPlayingChannel.groupTitle }
    }

    // Volume & Brightness state controls
    val activity = context as? Activity
    var currentBrightness by remember {
        mutableStateOf(activity?.window?.attributes?.screenBrightness ?: 0.5f)
    }
    if (currentBrightness < 0f) currentBrightness = 0.5f // auto fallback
    
    var currentVolume by remember { mutableStateOf(1f) }

    // Sleep Timer States
    var sleepTimerMinutesLeft by remember { mutableStateOf(0) }
    var isSleepTimerRunning by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // Stats overlay state
    var showStatsOverlay by remember { mutableStateOf(false) }

    fun setBrightnessValue(value: Float) {
        currentBrightness = value
        activity?.window?.let { window ->
            val layoutParams = window.attributes
            layoutParams.screenBrightness = value
            window.attributes = layoutParams
        }
    }

    fun setVolumeValue(value: Float) {
        currentVolume = value
        exoPlayer?.volume = value
    }

    // Screen locking properties – keeps screen on while streaming video
    DisposableEffect(key1 = true) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Media3 ExoPlayer setup
    var isPlaying by remember { mutableStateOf(true) }
    var currentResizeMode by remember { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) } 
    var showControls by remember { mutableStateOf(true) }
    var streamErrorOccurred by remember { mutableStateOf(false) }

    // Video quality selection states & presets
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedQualityPresetLabel by rememberSaveable { mutableStateOf("تلقائي (Auto)") }

    val qualityPresets = remember {
        listOf(
            QualityPreset("تلقائي (Auto)", Int.MAX_VALUE, Int.MAX_VALUE, "أفضل جودة تلقائية للاتصال / Best adaptive quality"),
            QualityPreset("Full HD (1080p)", 1920, 1080, "دقة فائقة الوضوح / Ultra High Definition"),
            QualityPreset("HD (720p)", 1280, 720, "دقة عالية / High Definition"),
            QualityPreset("SD (480p)", 854, 480, "دقة متوسطة موفرة للبيانات / Standard Definition"),
            QualityPreset("Low (360p)", 640, 360, "جودة منخفضة لسرعات الانترنت الضعيفة / Low stream quality")
        )
    }

    // Retrieve active track list if stream offers multiple hardcoded alternatives
    val detectedTrackQualities = remember(exoPlayer?.currentTracks) {
        val tracksList = mutableListOf<DetectedTrack>()
        exoPlayer?.currentTracks?.groups?.forEach { group ->
            if (group.type == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    if (group.isTrackSupported(i)) {
                        val format = group.getTrackFormat(i)
                        val label = "${format.height}p" + (if (format.frameRate > 0) " (${format.frameRate.toInt()}fps)" else "")
                        tracksList.add(
                            DetectedTrack(
                                group = group,
                                index = i,
                                label = label,
                                bitrate = format.bitrate,
                                isSelected = group.isTrackSelected(i)
                            )
                        )
                    }
                }
            }
        }
        tracksList.sortByDescending { it.bitrate }
        tracksList
    }

    fun applyQualityPreset(preset: QualityPreset, player: ExoPlayer) {
        selectedQualityPresetLabel = preset.label
        val newParams = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(preset.maxWidth, preset.maxHeight)
            .build()
        player.trackSelectionParameters = newParams
    }

    fun selectDetectedTrack(detectedTrack: DetectedTrack, player: ExoPlayer) {
        selectedQualityPresetLabel = "مخصص (${detectedTrack.label})"
        val override = TrackSelectionOverride(detectedTrack.group.mediaTrackGroup, detectedTrack.index)
        val newParams = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .addOverride(override)
            .build()
        player.trackSelectionParameters = newParams
    }

    // Favorited status
    val isFav = favorites.any { it.id == currentPlayingChannel.id }
    val currentEpg = remember(currentPlayingChannel.name) { getMockEpgForChannel(currentPlayingChannel.name) }

    // Manage life cycle of ExoPlayer safely
    DisposableEffect(context) {
        val player = ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                streamErrorOccurred = true
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        exoPlayer = player

        onDispose {
            player.removeListener(listener)
            player.release()
            exoPlayer = null
        }
    }

    // Initialize stream volume when player instantiates
    LaunchedEffect(exoPlayer) {
        exoPlayer?.let {
            currentVolume = it.volume
        }
    }

    // Sleep Timer countdown processor loop
    LaunchedEffect(isSleepTimerRunning, sleepTimerMinutesLeft) {
        if (isSleepTimerRunning && sleepTimerMinutesLeft > 0) {
            delay(60000L) // Wait 1 minute
            sleepTimerMinutesLeft -= 1
            if (sleepTimerMinutesLeft <= 0) {
                isSleepTimerRunning = false
                exoPlayer?.pause()
                isPlaying = false
                onBack() // Exit and stop resource use
            }
        }
    }

    // Start playback when channel shifts or player is ready
    LaunchedEffect(currentPlayingChannel, exoPlayer) {
        val player = exoPlayer ?: return@LaunchedEffect
        streamErrorOccurred = false
        try {
            val mediaItem = MediaItem.fromUri(currentPlayingChannel.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            viewModel.addChannelToHistory(currentPlayingChannel)
        } catch (e: Exception) {
            Log.e("IPTVPlayer", "Error loading uri: ${currentPlayingChannel.url}", e)
            streamErrorOccurred = true
        }
    }

    // Auto fade controls overlay
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(6000)
            showControls = false
        }
    }

    BackHandler {
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { showControls = !showControls }
    ) {
        // Video Native Renderer Canvas
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = exoPlayer
                        useController = false
                        this.resizeMode = currentResizeMode
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = currentResizeMode
                    playerView.player = exoPlayer
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay 1: Error indicators
        if (streamErrorOccurred) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SignalWifiStatusbarConnectedNoInternet4,
                        contentDescription = "Playback Error",
                        tint = PremiumRed,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Unable to stream channel.",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The connection timed out or the format is incompatible.",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            exoPlayer?.prepare()
                            exoPlayer?.play()
                            streamErrorOccurred = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumRed)
                    ) {
                        Text("Retry Connection")
                    }
                }
            }
        }

        // Overlay 2: Premium UI Video Controllers
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.75f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp)
            ) {
                // UPPER CONTROL HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (currentPlayingChannel.logoUrl != null) {
                            AsyncImage(
                                model = currentPlayingChannel.logoUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = currentPlayingChannel.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Beautiful Live EPG indicator on top bar
                            Text(
                                text = "الآن: ${currentEpg.title}",
                                color = AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Top Action elements
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // LIVE Glow indicators
                        Surface(
                            color = LiveIndicatorColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.player_live),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Toggle Favorites hearts
                        IconButton(onClick = { viewModel.toggleFavorite(currentPlayingChannel) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFav) PremiumRed else Color.White
                            )
                        }
                    }
                }

                // CENTER PLAYBACK TOGGLE BUTTON
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .align(Alignment.Center)
                        .clickable {
                            exoPlayer?.let { player ->
                                if (isPlaying) player.pause() else player.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Control",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // BOTTOM CONTROLLERS BAR
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Premium Level Mix slider panels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brightness Control
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = "Brightness",
                                tint = AccentOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = currentBrightness,
                                onValueChange = { setBrightnessValue(it) },
                                valueRange = 0.05f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = AccentOrange,
                                    activeTrackColor = AccentOrange,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Volume Control
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (currentVolume == 0f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = PremiumRed,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = currentVolume,
                                onValueChange = { setVolumeValue(it) },
                                valueRange = 0.0f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PremiumRed,
                                    activeTrackColor = PremiumRed,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Control tool selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Aspect Ratio Cycler
                        PlayerIconButton(
                            icon = Icons.Default.AspectRatio,
                            label = when (currentResizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Original"
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                                else -> "Fit"
                            }
                        ) {
                            currentResizeMode = when (currentResizeMode) {
                                AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                                AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            }
                        }

                        // 2. Quick Channels Navigation Drawer Trigger
                        PlayerIconButton(
                            icon = Icons.Default.FormatListBulleted,
                            label = if (configuration.locales[0].language == "ar") "قائمة القنوات" else "Channels Menu"
                        ) {
                            showQuickChannelsDrawer = !showQuickChannelsDrawer
                        }

                        // 3. Audio / Video Qualities Tracker Toggle
                        PlayerIconButton(
                            icon = Icons.Default.Tune,
                            label = "${stringResource(R.string.player_quality)}: $selectedQualityPresetLabel"
                        ) {
                            showQualityDialog = true
                        }

                        // 4. Sleep Timer Trigger Toggle
                        PlayerIconButton(
                            icon = Icons.Default.Schedule,
                            label = if (isSleepTimerRunning) {
                                if (configuration.locales[0].language == "ar") "مؤقت: ${sleepTimerMinutesLeft}د" else "Timer: ${sleepTimerMinutesLeft}m"
                            } else {
                                if (configuration.locales[0].language == "ar") "مؤقت النوم" else "Sleep Timer"
                            }
                        ) {
                            showSleepTimerDialog = true
                        }

                        // 5. Technical Stats Trigger Toggle
                        PlayerIconButton(
                            icon = Icons.Default.Info,
                            label = if (configuration.locales[0].language == "ar") "بيانات البث" else "Stream Stats"
                        ) {
                            showStatsOverlay = !showStatsOverlay
                        }
                    }
                }
            }
        }

        // Sliding Quick Channels Navigation Drawer
        AnimatedVisibility(
            visible = showQuickChannelsDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(300.dp)
                .background(Color.Black.copy(alpha = 0.88f))
                .border(BorderStroke(1.dp, PremiumRed.copy(alpha = 0.25f)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (configuration.locales[0].language == "ar") "التنقل السريع للقنوات" else "Quick Channel Switch",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showQuickChannelsDrawer = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = PremiumRed)
                    }
                }
                
                Divider(color = Color.White.copy(alpha = 0.12f), modifier = Modifier.padding(bottom = 12.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(groupChannels, key = { it.id }) { ch ->
                        val isCurrent = ch.id == currentPlayingChannel.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) PremiumRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp, 
                                if (isCurrent) PremiumRed else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentPlayingChannel = ch
                                    showQuickChannelsDrawer = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ch.logoUrl != null) {
                                    AsyncImage(
                                        model = ch.logoUrl,
                                        contentDescription = ch.name,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.White.copy(alpha = 0.05f))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PremiumRed.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = PremiumRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(10.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.name,
                                        color = if (isCurrent) PremiumRed else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val mockEpg = getMockEpgForChannel(ch.name)
                                    Text(
                                        text = mockEpg.title,
                                        color = TextSecondaryDark,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Beautiful Premium M3 Video Quality Selection Dialog
        if (showQualityDialog && exoPlayer != null) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                containerColor = CardSurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, PremiumRed.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .widthIn(max = 480.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Quality Options",
                            tint = PremiumRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (configuration.locales[0].language == "ar") "تحديد جودة الفيديو" else "Video Quality Selection",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 1. Adaptive Quality Presets
                        item {
                            Text(
                                text = if (configuration.locales[0].language == "ar") "خيارات تحديد دقة البث (تلقائي / متكيف)" else "Resolution Preference Limit (Adaptive Presets)",
                                color = PremiumRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
                            )
                        }

                        items(qualityPresets) { preset ->
                            val isSelected = selectedQualityPresetLabel == preset.label
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PremiumRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PremiumRed else Color.White.copy(alpha = 0.08f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        applyQualityPreset(preset, exoPlayer!!)
                                        showQualityDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.label,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = preset.description,
                                            color = TextSecondaryDark,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = PremiumRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Detected Explicit Stream Tracks (if HLS multi-bitrate provides any)
                        if (detectedTrackQualities.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (configuration.locales[0].language == "ar") "مسارات البث الفردية المكتشفة" else "Detected Stream Quality Tracks",
                                    color = PremiumRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(detectedTrackQualities) { track ->
                                val isSelected = selectedQualityPresetLabel == "مخصص (${track.label})"
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) PremiumRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) PremiumRed else Color.White.copy(alpha = 0.08f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectDetectedTrack(track, exoPlayer!!)
                                            showQualityDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.label,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Bitrate: ${(track.bitrate / 1000)} kbps",
                                                color = TextSecondaryDark,
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = PremiumRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text(
                            text = if (configuration.locales[0].language == "ar") "إغلاق" else "Close",
                            color = PremiumRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // Technical Stats Panel (Stats for Nerds / بيانات البث)
        AnimatedVisibility(
            visible = showStatsOverlay,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 80.dp, start = 16.dp)
                .width(280.dp)
        ) {
            val videoWidth = exoPlayer?.videoSize?.width ?: 0
            val videoHeight = exoPlayer?.videoSize?.height ?: 0
            val videoRes = if (videoWidth > 0 && videoHeight > 0) "${videoWidth}x${videoHeight}" else "1920x1080 (HD)"
            
            val isPlayingState = exoPlayer?.isPlaying ?: false
            val bufferValue = exoPlayer?.bufferedPercentage ?: 0
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.82f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PremiumRed.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.clickable { showStatsOverlay = false }
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (configuration.locales[0].language == "ar") "بيانات البث الفنية" else "Live Streaming Stats",
                            color = PremiumRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                    }
                    
                    Divider(color = Color.White.copy(alpha = 0.15f), thickness = 0.5.dp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Format (الدقة):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(videoRes, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Codec (الترميز):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("H.264 / AVC (MPEG-4)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Audio (الصوت):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("AAC / Stereo (48kHz)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Buffer (التخزين):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("${bufferValue}% Buffered", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status (الحالة):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text(
                            text = if (isPlayingState) "PLAYING" else "BUFFERING/PAUSED",
                            color = if (isPlayingState) LiveIndicatorColor else PremiumRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Decoder (المشغل):", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        Text("Hardware Accelerated", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // Beautiful Dialog for Sleep Timer Selection
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = CardSurfaceDark,
                modifier = Modifier.border(1.dp, PremiumRed.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
                title = {
                    Text(
                        text = if (configuration.locales[0].language == "ar") "مؤقت إطفاء البث" else "Sleep Timer Setup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (configuration.locales[0].language == "ar") 
                                "اختر الوقت المناسب لإيقاف تشغيل البث تلقائياً وتوفير استهلاك البيانات والطاقة:" 
                            else 
                                "Automatically pause streaming and save power & mobile data after selected duration:",
                            color = TextSecondaryDark,
                            fontSize = 13.sp
                        )
                        
                        val sleepOptions = listOf(
                            0 to if (configuration.locales[0].language == "ar") "إيقاف التشغيل" else "Turn Off Timer",
                            15 to if (configuration.locales[0].language == "ar") "بعد 15 دقيقة" else "In 15 Minutes",
                            30 to if (configuration.locales[0].language == "ar") "بعد 30 دقيقة" else "In 30 Minutes",
                            45 to if (configuration.locales[0].language == "ar") "بعد 45 دقيقة" else "In 45 Minutes",
                            60 to if (configuration.locales[0].language == "ar") "بعد ساعة واحدة" else "In 1 Hour"
                        )
                        
                        sleepOptions.forEach { (mins, label) ->
                            val isSelected = (mins == sleepTimerMinutesLeft && isSleepTimerRunning) || (mins == 0 && !isSleepTimerRunning)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PremiumRed.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        if (mins == 0) {
                                            isSleepTimerRunning = false
                                            sleepTimerMinutesLeft = 0
                                        } else {
                                            sleepTimerMinutesLeft = mins
                                            isSleepTimerRunning = true
                                        }
                                        showSleepTimerDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = label, color = if (isSelected) PremiumRed else Color.White, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = PremiumRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text(if (configuration.locales[0].language == "ar") "إغلاق" else "Close", color = PremiumRed, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun PlayerIconButton(
    imageVector: ImageVector? = null,
    icon: ImageVector? = null,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Icon(
            imageVector = icon ?: imageVector ?: Icons.Default.Help,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ==========================================
// 10. SIMULATED ELECTRONIC PROGRAM GUIDE (EPG)
// ==========================================
data class EpgProgram(
    val title: String,
    val subtitle: String,
    val startTime: String,
    val endTime: String,
    val progress: Float
)

fun getMockEpgForChannel(channelName: String): EpgProgram {
    val nameLower = channelName.lowercase()
    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val currentMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
    
    val progressVal = (currentMinute % 60) / 60f
    val startHourStr = String.format("%02d:00", currentHour)
    val endHourStr = String.format("%02d:00", (currentHour + 1) % 24)

    return when {
        nameLower.contains("news") || nameLower.contains("جزير") || nameLower.contains("عرب") || nameLower.contains("حدث") || nameLower.contains("أخبار") || nameLower.contains("بي بي سي") -> {
            EpgProgram(
                title = if (nameLower.contains("جزير") || nameLower.contains("عرب") || nameLower.contains("حدث") || nameLower.contains("أخبار")) "البث مباشر - تغطية النشرات الإخبارية" else "Live Global News Bulletin Desk",
                subtitle = "التالي: التحليل السياسي والتقارير",
                startTime = startHourStr,
                endTime = endHourStr,
                progress = progressVal
            )
        }
        nameLower.contains("mbc") || nameLower.contains("rotana") || nameLower.contains("روتانا") || nameLower.contains("drama") || nameLower.contains("دراما") || nameLower.contains("سينما") || nameLower.contains("فن") -> {
            EpgProgram(
                title = if (nameLower.contains("روتانا") || nameLower.contains("دراما") || nameLower.contains("mbc") || nameLower.contains("سينما")) "المسلسل الدرامي العربي اليومي" else "Prime Blockbuster Cinema Special",
                subtitle = "التالي: فيلم السهرة الحصري لليوم",
                startTime = startHourStr,
                endTime = endHourStr,
                progress = progressVal
            )
        }
        nameLower.contains("sport") || nameLower.contains("bein") || nameLower.contains("بين") || nameLower.contains("كأس") || nameLower.contains("رياض") || nameLower.contains("كرة") -> {
            EpgProgram(
                title = if (nameLower.contains("bein") || nameLower.contains("رياض") || nameLower.contains("بين") || nameLower.contains("كأس")) "الدوري الأوروبي والمحلي - الاستوديو التحليلي المباشر" else "Live Matches Studio analysis Arena",
                subtitle = "التالي: ملخص ومجريات اللقاءات الرياضية",
                startTime = startHourStr,
                endTime = endHourStr,
                progress = progressVal
            )
        }
        nameLower.contains("kid") || nameLower.contains("cartoon") || nameLower.contains("spacetoon") || nameLower.contains("براعم") || nameLower.contains("أطفال") -> {
            EpgProgram(
                title = if (nameLower.contains("سبيس") || nameLower.contains("أطفال") || nameLower.contains("براعم")) "فقرة كرتون الأطفال الممتعة والمترجمة" else "Magical Children Cartoon Blocks",
                subtitle = "التالي: برنامج مسابقات ترفيهي وعلمي للأطفال",
                startTime = startHourStr,
                endTime = endHourStr,
                progress = progressVal
            )
        }
        else -> {
            EpgProgram(
                title = if (channelName.matches(Regex(".*[\\u0600-\\u06FF]+.*"))) "برنامج المنوعات التلفزيوني المباشر" else "Continuous TV Broadcast Stream Showcase",
                subtitle = "التالي: الفيلم الوثائقي الثقافي المميّز",
                startTime = startHourStr,
                endTime = endHourStr,
                progress = progressVal
            )
        }
    }
}

// ==========================================
// DESIGNS REUSABLE ELEMENS COMPOSABLES
// ==========================================

@Composable
fun ChannelCircularIconItem(
    channel: ChannelEntity,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) CardSurfaceDark else Color.White)
                .border(
                    1.5.dp, 
                    PremiumRed.copy(alpha = 0.5f), 
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            } else {
                Icon(Icons.Default.Tv, contentDescription = null, tint = PremiumRed, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = channel.name,
            color = if (isDarkMode) Color.White else TextPrimaryLight,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ChannelPreviewCard(
    channel: ChannelEntity,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) CardSurfaceDark else Color.White
        ),
        border = BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(40.dp)
                        .weight(1f)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = PremiumRed,
                    modifier = Modifier
                        .size(34.dp)
                        .weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = channel.name,
                color = if (isDarkMode) Color.White else TextPrimaryLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ChannelInteractiveGridCard(
    channel: ChannelEntity,
    isDarkMode: Boolean,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val epg = remember(channel.name) { getMockEpgForChannel(channel.name) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) CardSurfaceDark else Color.White
        ),
        border = BorderStroke(
            1.dp, 
            if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Heart button layered globally in the top corner (with a dedicated touch target to prevent launching player)
            if (onFavoriteToggle != null) {
                IconButton(
                    onClick = { onFavoriteToggle() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) PremiumRed else (if (isDarkMode) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.35f)),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (channel.logoUrl != null) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(4.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PremiumRed.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = PremiumRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Column(modifier = Modifier.padding(end = 28.dp)) { // spacing to clear heart icon
                        Text(
                            text = channel.name,
                            color = if (isDarkMode) Color.White else TextPrimaryLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.groupTitle,
                            color = if (isDarkMode) TextSecondaryDark.copy(alpha = 0.8f) else TextSecondaryLight,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Simulated EPG Now Watching line and subtle bar
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "الآن: ${epg.title}",
                        color = if (isDarkMode) AccentOrange.copy(alpha = 0.9f) else DarkCrimson,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    LinearProgressIndicator(
                        progress = { epg.progress },
                        color = PremiumRed,
                        trackColor = if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.player_live),
                        color = LiveIndicatorColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch",
                        tint = PremiumRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// Side Nav bar child for Tablet structure
@Composable
fun NavRailItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isDarkMode: Boolean
) {
    NavigationRailItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon, 
                contentDescription = label,
                tint = if (selected) PremiumRed else (if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f))
            )
        },
        label = { Text(label, fontSize = 11.sp, color = if (selected) PremiumRed else (if (isDarkMode) TextSecondaryDark else TextSecondaryLight)) },
        colors = NavigationRailItemDefaults.colors(indicatorColor = PremiumRed.copy(alpha = 0.15f))
    )
}

// Bottom Nav bar child for Portable layout
@Composable
fun RowScope.BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    isDarkMode: Boolean
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon, 
                contentDescription = label,
                tint = if (selected) PremiumRed else (if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f))
            )
        },
        label = { Text(label, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (selected) PremiumRed else (if (isDarkMode) TextSecondaryDark else TextSecondaryLight)) },
        colors = NavigationBarItemDefaults.colors(indicatorColor = PremiumRed.copy(alpha = 0.15f))
    )
}

// Custom Colors definitions for forms/text fields
@Composable
fun transparentTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PremiumRed,
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    focusedLabelColor = PremiumRed,
    unfocusedLabelColor = TextSecondaryDark,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = PremiumRed
)

@Composable
fun searchTextFieldColors(isDarkMode: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PremiumRed,
    unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
    focusedLabelColor = PremiumRed,
    unfocusedLabelColor = TextSecondaryDark,
    focusedTextColor = if (isDarkMode) Color.White else TextPrimaryLight,
    unfocusedTextColor = if (isDarkMode) Color.White else TextPrimaryLight,
    cursorColor = PremiumRed,
    focusedContainerColor = if (isDarkMode) CardSurfaceDark else Color.White,
    unfocusedContainerColor = if (isDarkMode) CardSurfaceDark else Color.White
)
