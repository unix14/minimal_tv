package com.unix14.minimaltv.data.repository

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.HdmiCardItem
import com.unix14.minimaltv.domain.repository.AppRepository
import com.unix14.minimaltv.util.HomeOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

class AppRepositoryImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : AppRepository {

    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, Drawable>(200)
    private val rawAppsFlow = MutableStateFlow<List<AppModel>>(emptyList())
    private val moveMutex = Mutex()

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

            val existingAppsMap = rawAppsFlow.value.associateBy { it.packageName }
            val combinedMap = linkedMapOf<String, AppModel>()

            for (resolveInfo in leanbackList) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == selfPkg) continue
                val existing = existingAppsMap[pkg]
                if (existing != null) {
                    combinedMap[pkg] = existing
                    continue
                }
                val actName = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()

                val cachedBanner = iconCache.get("${pkg}_banner")
                val banner = cachedBanner ?: resolveInfo.activityInfo.loadBanner(packageManager)?.also {
                    iconCache.put("${pkg}_banner", it)
                }

                val cachedIcon = iconCache.get("${pkg}_icon")
                val icon = cachedIcon ?: resolveInfo.activityInfo.loadIcon(packageManager)?.also {
                    iconCache.put("${pkg}_icon", it)
                }

                combinedMap[pkg] = AppModel(
                    packageName = pkg,
                    activityName = actName,
                    label = label,
                    banner = banner,
                    icon = icon
                )
            }

            for (resolveInfo in standardList) {
                val pkg = resolveInfo.activityInfo.packageName
                if (pkg == selfPkg || combinedMap.containsKey(pkg)) continue
                val existing = existingAppsMap[pkg]
                if (existing != null) {
                    combinedMap[pkg] = existing
                    continue
                }
                val actName = resolveInfo.activityInfo.name
                val label = resolveInfo.loadLabel(packageManager).toString()

                val cachedIcon = iconCache.get("${pkg}_icon")
                val icon = cachedIcon ?: resolveInfo.activityInfo.loadIcon(packageManager)?.also {
                    iconCache.put("${pkg}_icon", it)
                }

                combinedMap[pkg] = AppModel(
                    packageName = pkg,
                    activityName = actName,
                    label = label,
                    banner = null,
                    icon = icon
                )
            }

            val newApps = combinedMap.values.toList()
            if (rawAppsFlow.value.map { it.packageName } != newApps.map { it.packageName }) {
                rawAppsFlow.value = newApps
            }

            val isInit = preferencesRepository.initialFavoritesInitializedFlow.first()
            val currentFavs = preferencesRepository.favoritesFlow.first()
            if (!isInit || currentFavs.isEmpty()) {
                initDefaultFavorites(combinedMap.keys.toList())
            }
        }
    }

    private suspend fun initDefaultFavorites(installedPackages: List<String>) {
        val curatedMediaApps = listOf(
            "org.smarttube.stable",
            "com.stremio.one",
            "org.jellyfin.androidtv",
            "com.netflix.ninja",
            "com.apple.atve.androidtv.appletv",
            "com.spotify.tv.android",
            "com.keshet.mako.VODAndroidTV",
            "com.applicaster.iReshet",
            "com.applicaster.il.ch1",
            "com.streamisrael",
            "com.google.android.youtube.tv",
            "com.android.vending",
            "com.alphainventor.filemanager"
        )

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -90)
        val startTime = calendar.timeInMillis
        val statsList = try {
            usageStatsManager?.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startTime, endTime)
        } catch (e: Exception) {
            null
        }

        val usageMap = statsList?.associate { it.packageName to (it.totalTimeInForeground + it.lastTimeUsed) } ?: emptyMap()
        val hasUsage = usageMap.values.any { it > 0 }

        val rankedList = if (hasUsage) {
            installedPackages.sortedWith(
                compareByDescending<String> { usageMap[it] ?: 0L }
                    .thenBy {
                        val idx = curatedMediaApps.indexOf(it)
                        if (idx != -1) idx else Int.MAX_VALUE
                    }
                    .thenBy { it.lowercase() }
            )
        } else {
            installedPackages.sortedWith(
                compareBy<String> {
                    val idx = curatedMediaApps.indexOf(it)
                    if (idx != -1) idx else Int.MAX_VALUE
                }.thenBy { it.lowercase() }
            )
        }

        val top10 = rankedList.take(10)
        preferencesRepository.initDefaultFavorites(top10)
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

    override suspend fun moveApp(key: String, direction: Int) {
        moveMutex.withLock {
            withContext(Dispatchers.IO) {
                val savedOrder = preferencesRepository.appOrderFlow.first()
                val favoriteApps = getFavoriteApps().first()
                val displayed = HomeOrder.sort(favoriteApps, HdmiCardItem.defaults(), savedOrder)
                val keys = displayed.mapNotNull { HomeOrder.keyOf(it) }.toMutableList()
                val currentIndex = keys.indexOf(key)
                if (currentIndex == -1) return@withContext
                val targetIndex = currentIndex + direction
                if (targetIndex !in keys.indices) return@withContext
                keys.add(targetIndex, keys.removeAt(currentIndex))
                val remainder = savedOrder.filter { it !in keys }
                preferencesRepository.saveAppOrder(keys + remainder)
            }
        }
    }
}
