package com.unix14.minimaltv

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.graphics.Bitmap
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
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

class MinimalTVApp : Application(), ImageLoaderFactory {

    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var appRepository: AppRepository
        private set
    lateinit var channelRepository: ChannelRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowHardware(true)
            .crossfade(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        com.unix14.minimaltv.util.Motion.init(this)

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
