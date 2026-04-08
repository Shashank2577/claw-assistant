package com.phoneclaw.ai.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Chat : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object VoiceConversation : Screen

    @Serializable
    data object FileBrowser : Screen

    @Serializable
    data object Onboarding : Screen
}

sealed interface BottomSheet {
    data object ModelPicker : BottomSheet
    data class PerChatSettings(val conversationId: String) : BottomSheet
}
