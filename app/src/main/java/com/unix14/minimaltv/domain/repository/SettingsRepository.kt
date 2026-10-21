package com.unix14.minimaltv.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getAppLocale(): Flow<String>
    suspend fun setAppLocale(locale: String)
    fun isAccessibilityEnabled(): Boolean
}
