package com.unix14.minimaltv.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AppRepositoryImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : AppRepository {

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Drawable>(150)
    private val rawAppsFlow = MutableStateFlow<List<AppModel>>(emptyList())

    override fun getInstalledApps(): Flow<List<AppModel>> {
        return combine(
            rawAppsFlow,
            preferencesRepository.favoritesFlow,
            preferencesRepository.hiddenAppsFlow,
            preferencesRepository.appOrderFlow
        ) { rawApps, favorites, hiddenApps, customOrder ->
            val visibleApps = rawApps.filter { !hiddenApps.contains(it.packageName) }
            val sortedApps = if (customOrder.isNotEmpty()) {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                visibleApps.sortedWith(compareBy({ orderMap[it.packageName] ?: Int.MAX_VALUE }, { it.label.lowercase() }))
            } else {
                visibleApps.sortedBy { it.label.lowercase() }
            }
            sortedApps.map { app ->
                app.copy(isFavorite = favorites.contains(app.packageName))
            }
        }
    }

    override fun getFavoriteApps(): Flow<List<AppModel>> {
        return combine(
            rawAppsFlow,
            preferencesRepository.favoritesFlow,
            preferencesRepository.hiddenAppsFlow,
            preferencesRepository.appOrderFlow
        ) { rawApps, favorites, hiddenApps, customOrder ->
            val favApps = rawApps.filter { favorites.contains(it.packageName) && !hiddenApps.contains(it.packageName) }
            if (customOrder.isNotEmpty()) {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                favApps.sortedWith(compareBy({ orderMap[it.packageName] ?: Int.MAX_VALUE }, { it.label.lowercase() }))
            } else {
                favApps.sortedBy { it.label.lowercase() }
            }.map { it.copy(isFavorite = true) }
        }
    }

    override suspend fun refreshApps() {
        withContext(Dispatchers.IO) {
            val selfPkg = context.packageName
            val leanbackIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PackageManager.MATCH_ALL else 0
            val leanbackList = packageManager.queryIntentActivities(leanbackIntent, flags)

            val standardIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val standardList = packageManager.queryIntentActivities(standardIntent, flags)

            val combinedMap = linkedMapOf<String, AppModel>()

            for (resolveInfo in leanbackList) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == selfPkg) continue
                val actName = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()
                val cached = iconCache.get(pkg)
                val (drawable, isBanner) = if (cached != null) {
                    cached to true
                } else {
                    val banner = resolveInfo.activityInfo.loadBanner(packageManager)
                    if (banner != null) {
                        iconCache.put(pkg, banner)
                        banner to true
                    } else {
                        val icon = resolveInfo.activityInfo.loadIcon(packageManager)
                        iconCache.put(pkg, icon)
                        icon to false
                    }
                }
                combinedMap[pkg] = AppModel(
                    packageName = pkg,
                    activityName = actName,
                    label = label,
                    bannerOrIcon = drawable,
                    isBanner = isBanner
                )
            }

            for (resolveInfo in standardList) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == selfPkg || combinedMap.containsKey(pkg)) continue
                val actName = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()
                val cached = iconCache.get(pkg)
                val drawable = cached ?: run {
                    val icon = resolveInfo.activityInfo.loadIcon(packageManager)
                    iconCache.put(pkg, icon)
                    icon
                }
                combinedMap[pkg] = AppModel(
                    packageName = pkg,
                    activityName = actName,
                    label = label,
                    bannerOrIcon = drawable,
                    isBanner = false
                )
            }

            rawAppsFlow.value = combinedMap.values.toList()
        }
    }

    override suspend fun pinFavorite(packageName: String) {
        preferencesRepository.setFavorite(packageName, true)
    }

    override suspend fun unpinFavorite(packageName: String) {
        preferencesRepository.setFavorite(packageName, false)
    }

    override suspend fun hideApp(packageName: String) {
        preferencesRepository.setHidden(packageName, true)
    }

    override suspend fun unhideApp(packageName: String) {
        preferencesRepository.setHidden(packageName, false)
    }

    override suspend fun moveApp(packageName: String, direction: Int) {
        withContext(Dispatchers.IO) {
            val currentList = rawAppsFlow.value.map { it.packageName }.toMutableList()
            val currentIndex = currentList.indexOf(packageName)
            if (currentIndex == -1) return@withContext
            val targetIndex = currentIndex + direction
            if (targetIndex in 0 until currentList.size) {
                val item = currentList.removeAt(currentIndex)
                currentList.add(targetIndex, item)
                preferencesRepository.saveAppOrder(currentList)
            }
        }
    }
}
