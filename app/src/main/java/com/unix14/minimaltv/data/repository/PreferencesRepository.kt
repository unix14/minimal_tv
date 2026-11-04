package com.unix14.minimaltv.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "minimal_tv_prefs")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
        val APP_ORDER = stringPreferencesKey("app_order")
        val ENABLED_CHANNELS = stringSetPreferencesKey("enabled_channels")
        val CHANNEL_ORDER = stringPreferencesKey("channel_order")
        val APP_LOCALE = stringPreferencesKey("app_locale")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val INITIAL_FAVORITES_INITIALIZED = booleanPreferencesKey("initial_favorites_initialized")
    }

    val favoritesFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.FAVORITES] ?: emptySet() }

    val hiddenAppsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.HIDDEN_APPS] ?: emptySet() }

    val appOrderFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val raw = preferences[PreferencesKeys.APP_ORDER] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        }

    val enabledChannelsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.ENABLED_CHANNELS] ?: emptySet() }

    val channelOrderFlow: Flow<List<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val raw = preferences[PreferencesKeys.CHANNEL_ORDER] ?: ""
            if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
        }

    val appLocaleFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.APP_LOCALE] ?: "system" }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }

    val initialFavoritesInitializedFlow: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.INITIAL_FAVORITES_INITIALIZED] ?: false }

    suspend fun setFavorite(packageName: String, isFavorite: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (isFavorite) current.add(packageName) else current.remove(packageName)
            preferences[PreferencesKeys.FAVORITES] = current
        }
    }

    suspend fun initDefaultFavorites(favoritesList: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAVORITES] = favoritesList.toSet()
            preferences[PreferencesKeys.APP_ORDER] = favoritesList.joinToString(",")
            preferences[PreferencesKeys.INITIAL_FAVORITES_INITIALIZED] = true
        }
    }

    suspend fun setHidden(packageName: String, isHidden: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.HIDDEN_APPS]?.toMutableSet() ?: mutableSetOf()
            if (isHidden) current.add(packageName) else current.remove(packageName)
            preferences[PreferencesKeys.HIDDEN_APPS] = current
        }
    }

    suspend fun saveAppOrder(orderList: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_ORDER] = orderList.joinToString(",")
        }
    }

    suspend fun setChannelEnabled(channelId: Long, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.ENABLED_CHANNELS]?.toMutableSet() ?: mutableSetOf()
            val idStr = channelId.toString()
            if (enabled) current.add(idStr) else current.remove(idStr)
            preferences[PreferencesKeys.ENABLED_CHANNELS] = current
        }
    }

    suspend fun saveChannelOrder(orderList: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHANNEL_ORDER] = orderList.joinToString(",")
        }
    }

    suspend fun setAppLocale(locale: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCALE] = locale
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}
