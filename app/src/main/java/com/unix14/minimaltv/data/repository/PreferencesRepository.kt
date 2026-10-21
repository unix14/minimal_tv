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
        val DISABLED_CHANNELS = stringSetPreferencesKey("disabled_channels")
        val APP_LOCALE = stringPreferencesKey("app_locale")
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

    val disabledChannelsFlow: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.DISABLED_CHANNELS] ?: emptySet() }

    val appLocaleFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences -> preferences[PreferencesKeys.APP_LOCALE] ?: "system" }

    suspend fun setFavorite(packageName: String, isFavorite: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.FAVORITES]?.toMutableSet() ?: mutableSetOf()
            if (isFavorite) current.add(packageName) else current.remove(packageName)
            preferences[PreferencesKeys.FAVORITES] = current
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
            val current = preferences[PreferencesKeys.DISABLED_CHANNELS]?.toMutableSet() ?: mutableSetOf()
            val idStr = channelId.toString()
            if (enabled) current.remove(idStr) else current.add(idStr)
            preferences[PreferencesKeys.DISABLED_CHANNELS] = current
        }
    }

    suspend fun setAppLocale(locale: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCALE] = locale
        }
    }
}
