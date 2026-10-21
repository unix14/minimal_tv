package com.unix14.minimaltv.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.unix14.minimaltv.domain.repository.AppRepository
import com.unix14.minimaltv.domain.repository.ChannelRepository
import com.unix14.minimaltv.domain.repository.SettingsRepository

class MainViewModelFactory(
    private val appRepository: AppRepository,
    private val channelRepository: ChannelRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(appRepository, channelRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
