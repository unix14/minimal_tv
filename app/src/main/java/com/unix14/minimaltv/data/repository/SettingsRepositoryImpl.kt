package com.unix14.minimaltv.data.repository

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.unix14.minimaltv.domain.repository.SettingsRepository
import com.unix14.minimaltv.service.HomeInterceptorAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : SettingsRepository {

    override fun getAppLocale(): Flow<String> {
        return preferencesRepository.appLocaleFlow
    }

    override suspend fun setAppLocale(locale: String) {
        preferencesRepository.setAppLocale(locale)
        withContext(Dispatchers.Main) {
            val localeList = if (locale == "system" || locale.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(locale)
            }
            AppCompatDelegate.setApplicationLocales(localeList)
        }
    }

    override fun isAccessibilityEnabled(): Boolean {
        val expectedComponentName = ComponentName(context, HomeInterceptorAccessibilityService::class.java).flattenToString()
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            if (componentNameString.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
