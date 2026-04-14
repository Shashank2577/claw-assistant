package com.phoneclaw.ai

import android.app.Application
import com.phoneclaw.ai.data.DataStoreRepository
import com.phoneclaw.ai.ui.theme.ThemeSettings
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class OpenClawApp : Application() {

    @Inject lateinit var dataStoreRepository: DataStoreRepository

    override fun onCreate() {
        super.onCreate()

        // Pre-create the WebView data directory to prevent a crash on first launch
        // where app_webview/webview_data.lock doesn't yet exist.
        File(dataDir, "app_webview").mkdirs()

        // Load saved theme faithfully from ported logic
        ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()
    }
}
