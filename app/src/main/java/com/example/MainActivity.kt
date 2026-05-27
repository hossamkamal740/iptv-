package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import com.example.data.ChannelEntity
import com.example.data.PlaylistEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val viewModel: IPTVViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                var currentScreen by remember { mutableStateOf(Screen.Splash) }
                var selectedChannelForPlayback by remember { mutableStateOf<ChannelEntity?>(null) }
                val playlists by viewModel.playlists.collectAsState()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // Navigation Switcher Routing
                    Crossfade(
                        targetState = currentScreen,
                        label = "MainScreenTransition"
                    ) { targetScreen ->
                        when (targetScreen) {
                            Screen.Splash -> {
                                SplashScreen(
                                    onNavigateToNext = {
                                        // If there are saved playlists, jump directly to dashboard, otherwise, show connection form!
                                        if (playlists.isNotEmpty()) {
                                            currentScreen = Screen.Dashboard
                                        } else {
                                            currentScreen = Screen.Setup
                                        }
                                    }
                                )
                            }
                            Screen.Setup -> {
                                SetupScreen(
                                    viewModel = viewModel,
                                    onNavigateToDashboard = {
                                        currentScreen = Screen.Dashboard
                                    }
                                )
                            }
                            Screen.Dashboard -> {
                                MainDashboard(
                                    viewModel = viewModel,
                                    onNavigateToPlayer = { channel ->
                                        selectedChannelForPlayback = channel
                                        currentScreen = Screen.Player
                                    },
                                    onNavigateToSetup = {
                                        currentScreen = Screen.Setup
                                    }
                                )
                            }
                            Screen.Player -> {
                                val channel = selectedChannelForPlayback
                                if (channel != null) {
                                    PlayerScreen(
                                        viewModel = viewModel,
                                        channel = channel,
                                        onBack = {
                                            currentScreen = Screen.Dashboard
                                            selectedChannelForPlayback = null
                                        }
                                    )
                                } else {
                                    currentScreen = Screen.Dashboard
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
