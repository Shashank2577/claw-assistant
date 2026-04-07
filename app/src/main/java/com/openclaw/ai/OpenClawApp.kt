package com.openclaw.ai

import android.app.Application
import com.openclaw.ai.data.DataStoreRepository
import com.openclaw.ai.ui.theme.ThemeSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OpenClawApp : Application() {

    @Inject lateinit var dataStoreRepository: DataStoreRepository

    override fun onCreate() {
        super.onCreate()
        
        // Load saved theme faithfully from ported logic
        ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()
    }
}
