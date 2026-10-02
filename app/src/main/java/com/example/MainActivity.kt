package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.model.UserRole
import com.example.ui.components.DorisioBottomNav
import com.example.ui.components.DorisioTopBar
import com.example.ui.screens.*
import com.example.ui.theme.CharcoalBg
import com.example.ui.theme.DorisioTheme
import com.example.viewmodel.DorisioViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {

    private val viewModel: DorisioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DorisioTheme {
                // Mandatory Arabic RTL Layout Direction
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()
                    val isFirebaseConfigured by viewModel.isFirebaseConfigured.collectAsState()
                    val isAdmin = currentUser?.role == UserRole.ADMIN

                    if (currentUser == null) {
                        AuthScreen(viewModel = viewModel)
                    } else {
                        // Handle back button on sub-screens
                        BackHandler(enabled = currentScreen != Screen.HOME) {
                            viewModel.navigateBack()
                        }

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CharcoalBg),
                            topBar = {
                                DorisioTopBar(
                                    currentScreen = currentScreen,
                                    isAdmin = isAdmin,
                                    isFirebaseConnected = isConfiguredFirebase(isFirebaseConfigured),
                                    onAdminClick = { viewModel.navigateTo(Screen.ADMIN_PANEL) },
                                    onFirebaseGuideClick = { viewModel.navigateTo(Screen.FIREBASE_SETUP_GUIDE) },
                                    onBackClick = { viewModel.navigateBack() }
                                )
                            },
                            bottomBar = {
                                // Show bottom navigation on primary tabs
                                if (currentScreen in listOf(Screen.HOME, Screen.MATCHES, Screen.STANDINGS, Screen.PLAYERS, Screen.PROFILE)) {
                                    DorisioBottomNav(
                                        currentScreen = currentScreen,
                                        isAdmin = isAdmin,
                                        onNavigate = { screen -> viewModel.navigateTo(screen) }
                                    )
                                }
                            },
                            contentWindowInsets = WindowInsets(0, 0, 0, 0)
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(CharcoalBg)
                            ) {
                                when (currentScreen) {
                                    Screen.HOME -> HomeScreen(viewModel = viewModel)
                                    Screen.MATCHES -> MatchesScreen(viewModel = viewModel)
                                    Screen.STANDINGS -> StandingsScreen(viewModel = viewModel)
                                    Screen.PLAYERS -> PlayersScreen(viewModel = viewModel)
                                    Screen.PROFILE -> ProfileScreen(viewModel = viewModel)
                                    Screen.MATCH_DETAILS -> MatchDetailsScreen(viewModel = viewModel)
                                    Screen.LIVE_ADMIN_CENTER -> if (isAdmin) LiveAdminCenterScreen(viewModel = viewModel) else HomeScreen(viewModel = viewModel)
                                    Screen.ADMIN_PANEL -> if (isAdmin) AdminPanelScreen(viewModel = viewModel) else HomeScreen(viewModel = viewModel)
                                    Screen.FIREBASE_SETUP_GUIDE -> FirebaseSetupScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isConfiguredFirebase(isConfig: Boolean): Boolean {
        return isConfig
    }
}
