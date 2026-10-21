package com.unix14.minimaltv.domain.repository

import com.unix14.minimaltv.domain.model.AppModel
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getInstalledApps(): Flow<List<AppModel>>
    fun getFavoriteApps(): Flow<List<AppModel>>
    suspend fun refreshApps()
    suspend fun pinFavorite(packageName: String)
    suspend fun unpinFavorite(packageName: String)
    suspend fun hideApp(packageName: String)
    suspend fun unhideApp(packageName: String)
    suspend fun moveApp(packageName: String, direction: Int)
}
