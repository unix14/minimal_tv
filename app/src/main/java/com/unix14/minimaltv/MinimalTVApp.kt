package com.unix14.minimaltv

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.unix14.minimaltv.data.repository.AppRepositoryImpl
import com.unix14.minimaltv.data.repository.ChannelRepositoryImpl
import com.unix14.minimaltv.data.repository.PreferencesRepository
import com.unix14.minimaltv.data.repository.SettingsRepositoryImpl
import com.unix14.minimaltv.domain.repository.AppRepository
import com.unix14.minimaltv.domain.repository.ChannelRepository
import com.unix14.minimaltv.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MinimalTVApp : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var channelRepository: ChannelRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferencesRepository = PreferencesRepository(this)
        appRepository = AppRepositoryImpl(this, preferencesRepository)
        channelRepository = ChannelRepositoryImpl(this, preferencesRepository)
        settingsRepository = SettingsRepositoryImpl(this, preferencesRepository)

        // Restore saved locale on start
        CoroutineScope(Dispatchers.Main).launch {
            val savedLocale = preferencesRepository.appLocaleFlow.first()
            if (savedLocale != "system" && savedLocale.isNotBlank()) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLocale))
            }
        }
    }

    companion object {
        lateinit var instance: MinimalTVApp
            private set
    }
}
