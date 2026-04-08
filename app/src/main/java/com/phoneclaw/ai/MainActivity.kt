package com.phoneclaw.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phoneclaw.ai.data.repository.ThemeMode
import com.phoneclaw.ai.ui.chat.ChatScreen
import com.phoneclaw.ai.ui.common.AppNavigationDrawer
import com.phoneclaw.ai.ui.modelpicker.ModelPickerSheet
import com.phoneclaw.ai.ui.onboarding.OnboardingScreen
import com.phoneclaw.ai.ui.settings.PerChatSettingsSheet
import com.phoneclaw.ai.ui.settings.SettingsScreen
import com.phoneclaw.ai.ui.theme.OpenClawAITheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var lifecycleProvider: AppLifecycleProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val isOnboardingComplete by viewModel.isOnboardingComplete.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            OpenClawAITheme(darkTheme = darkTheme) {
                if (!isOnboardingComplete) {
                    OnboardingScreen(onComplete = { viewModel.completeOnboarding() })
                } else {
                    MainContent(viewModel)
                }
            }
        }
    }
}

private sealed class BottomSheet {
    data object ModelPicker : BottomSheet()
    data class PerChatSettings(val conversationId: String) : BottomSheet()
}

@Composable
private fun MainContent(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentConversationId by mainViewModel.currentConversationId.collectAsStateWithLifecycle()

    var activeBottomSheet by remember { mutableStateOf<BottomSheet?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppNavigationDrawer(
                currentConversationId = currentConversationId,
                onConversationClick = { id ->
                    mainViewModel.setCurrentConversation(id)
                    navController.navigate("chat/$id") {
                        popUpTo("chat") { inclusive = false }
                    }
                    scope.launch { drawerState.close() }
                },
                onNewConversation = {
                    mainViewModel.setCurrentConversation(null)
                    navController.navigate("chat") {
                        popUpTo("chat") { inclusive = true }
                    }
                    scope.launch { drawerState.close() }
                },
                onSettingsClick = {
                    navController.navigate("settings")
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        NavHost(navController = navController, startDestination = "chat") {
            composable("chat") {
                ChatScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenModelPicker = { activeBottomSheet = BottomSheet.ModelPicker },
                    onOpenPerChatSettings = { id -> 
                        activeBottomSheet = BottomSheet.PerChatSettings(id) 
                    }
                )
            }
            composable("chat/{conversationId}") { backStackEntry ->
                val id = backStackEntry.arguments?.getString("conversationId") ?: ""
                ChatScreen(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onOpenModelPicker = { activeBottomSheet = BottomSheet.ModelPicker },
                    onOpenPerChatSettings = { activeBottomSheet = BottomSheet.PerChatSettings(id) }
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    // Bottom Sheets
    when (val sheet = activeBottomSheet) {
        is BottomSheet.ModelPicker -> {
            ModelPickerSheet(
                onDismiss = { activeBottomSheet = null },
                onManageModels = {
                    activeBottomSheet = null
                    navController.navigate("settings")
                }
            )
        }
        is BottomSheet.PerChatSettings -> {
            PerChatSettingsSheet(
                conversationId = sheet.conversationId,
                onDismiss = { activeBottomSheet = null },
            )
        }
        null -> Unit
    }
}
