package com.phoneclaw.ai.data.repository.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.phoneclaw.ai.data.db.dao.PerChatSettingsDao
import com.phoneclaw.ai.data.db.entity.PerChatSettingsEntity
import com.phoneclaw.ai.data.repository.SettingsRepository
import com.phoneclaw.ai.data.repository.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val perChatSettingsDao: PerChatSettingsDao,
) : SettingsRepository {

    private object Keys {
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override fun getGeminiApiKey(): Flow<String?> = dataStore.data.map { it[Keys.GEMINI_API_KEY] }

    override suspend fun setGeminiApiKey(key: String?) {
        dataStore.edit { prefs ->
            if (key == null) {
                prefs.remove(Keys.GEMINI_API_KEY)
            } else {
                prefs[Keys.GEMINI_API_KEY] = key
            }
        }
    }

    override suspend fun hasGeminiApiKey(): Boolean {
        val prefs = dataStore.data.first()
        return !prefs[Keys.GEMINI_API_KEY].isNullOrBlank()
    }

    override fun getThemeMode(): Flow<ThemeMode> = dataStore.data.map {
        val mode = it[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(mode)
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override fun getDefaultModelId(): Flow<String> = dataStore.data.map {
        it[Keys.DEFAULT_MODEL_ID] ?: ""
    }

    override suspend fun setDefaultModelId(id: String) {
        dataStore.edit { it[Keys.DEFAULT_MODEL_ID] = id }
    }

    override fun isOnboardingComplete(): Flow<Boolean> = dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun getPerChatSettings(conversationId: String): PerChatSettingsEntity? {
        return perChatSettingsDao.getPerChatSettings(conversationId)
    }

    override fun observePerChatSettings(conversationId: String): Flow<PerChatSettingsEntity?> {
        return perChatSettingsDao.observePerChatSettings(conversationId)
    }

    override suspend fun savePerChatSettings(settings: PerChatSettingsEntity) {
        perChatSettingsDao.insertPerChatSettings(settings)
    }

    override suspend fun deletePerChatSettings(conversationId: String) {
        perChatSettingsDao.deletePerChatSettings(conversationId)
    }
}
