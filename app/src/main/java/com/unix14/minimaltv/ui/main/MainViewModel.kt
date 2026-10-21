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
            while (isActive) {
                val now = Date()
                val currentLocale = Locale.getDefault()
                val timeFormat = SimpleDateFormat("HH:mm", currentLocale)
                val dateFormat = SimpleDateFormat("EEEE, MMM d", currentLocale)
                _currentTime.value = timeFormat.format(now)
                _currentDate.value = dateFormat.format(now)
                delay(1000L)
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

    fun moveApp(packageName: String, direction: Int) {
        viewModelScope.launch {
            appRepository.moveApp(packageName, direction)
        }
    }

    fun setChannelEnabled(channelId: Long, enabled: Boolean) {
        viewModelScope.launch {
            channelRepository.setChannelEnabled(channelId, enabled)
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
}
