package com.unix14.minimaltv.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.domain.repository.AppRepository
import com.unix14.minimaltv.domain.repository.ChannelRepository
import com.unix14.minimaltv.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(
    private val appRepository: AppRepository,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val installedApps: StateFlow<List<AppModel>> = appRepository.getInstalledApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteApps: StateFlow<List<AppModel>> = appRepository.getFavoriteApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeOrder: StateFlow<List<String>> = com.unix14.minimaltv.MinimalTVApp.instance.preferencesRepository.appOrderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val previewChannels: StateFlow<List<ChannelModel>> = channelRepository.getPreviewChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appLocale: StateFlow<String> = settingsRepository.getAppLocale()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    init {
        startClockTicker()
        loadData()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            var cachedLocale: Locale? = null
            var timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            var dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
            while (isActive) {
                val currentLocale = Locale.getDefault()
                if (currentLocale != cachedLocale) {
                    cachedLocale = currentLocale
                    timeFormat = SimpleDateFormat("HH:mm", currentLocale)
                    dateFormat = SimpleDateFormat("EEEE, MMM d", currentLocale)
                }
                val now = Date()
                val formattedTime = timeFormat.format(now)
                val formattedDate = dateFormat.format(now)
                if (_currentTime.value != formattedTime) {
                    _currentTime.value = formattedTime
                }
                if (_currentDate.value != formattedDate) {
                    _currentDate.value = formattedDate
                }
                delay(10_000L)
            }
        }
    }

    fun loadData() {
        viewModelScope.launch {
            appRepository.refreshApps()
            channelRepository.refreshChannels()
        }
    }

    fun pinFavorite(packageName: String) {
        viewModelScope.launch {
            appRepository.pinFavorite(packageName)
        }
    }

    fun unpinFavorite(packageName: String) {
        viewModelScope.launch {
            appRepository.unpinFavorite(packageName)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            appRepository.hideApp(packageName)
        }
    }

    /** [key] is an app package name or an HDMI shortcut key (see HdmiCardItem.orderKey). */
    fun moveApp(key: String, direction: Int) {
        viewModelScope.launch {
            appRepository.moveApp(key, direction)
        }
    }

    fun setChannelEnabled(channelId: Long, enabled: Boolean) {
        viewModelScope.launch {
            channelRepository.setChannelEnabled(channelId, enabled)
        }
    }

    fun moveChannel(channelId: Long, direction: Int) {
        viewModelScope.launch {
            channelRepository.moveChannel(channelId, direction)
        }
    }

    fun setAppLocale(locale: String) {
        viewModelScope.launch {
            settingsRepository.setAppLocale(locale)
        }
    }

    fun isAccessibilityEnabled(): Boolean {
        return settingsRepository.isAccessibilityEnabled()
    }

    val onboardingCompleted: StateFlow<Boolean> = com.unix14.minimaltv.MinimalTVApp.instance.preferencesRepository.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            com.unix14.minimaltv.MinimalTVApp.instance.preferencesRepository.setOnboardingCompleted(completed)
        }
    }
}
