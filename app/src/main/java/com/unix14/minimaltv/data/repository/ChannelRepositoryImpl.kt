package com.unix14.minimaltv.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.tvprovider.media.tv.TvContractCompat
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.domain.model.ProgramModel
import com.unix14.minimaltv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ChannelRepository {

    private val channelsStateFlow = MutableStateFlow<List<ChannelModel>>(emptyList())
    private val moveMutex = Mutex()

    override fun getPreviewChannels(): Flow<List<ChannelModel>> {
        return combine(channelsStateFlow, preferencesRepository.enabledChannelsFlow, preferencesRepository.channelOrderFlow) { channels, enabledIds, customOrder ->
            val mapped = channels.map { channel ->
                channel.copy(isEnabled = enabledIds.contains(channel.id.toString()))
            }
            if (customOrder.isNotEmpty()) {
                val orderMap = customOrder.withIndex().associate { it.value to it.index }
                mapped.sortedWith(compareBy({ orderMap[it.id.toString()] ?: Int.MAX_VALUE }, { it.appName }, { it.title.lowercase() }))
            } else {
                mapped.sortedWith(compareBy({ it.appName }, { it.title.lowercase() }))
            }
        }
    }

    override suspend fun refreshChannels() {
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val packageManager = context.packageManager
            val channelList = mutableListOf<ChannelModel>()

            // 1. Load Continue Watching / Watch Next programs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val watchNextPrograms = loadWatchNextPrograms()
                if (watchNextPrograms.isNotEmpty()) {
                    channelList.add(
                        ChannelModel(
                            id = -100L,
                            packageName = "android.media.tv",
                            title = "Continue Watching",
                            appName = "Watch Next",
                            programs = watchNextPrograms
                        )
                    )
                }
            }

            // 2. Query System Channels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val projection = arrayOf(
                        TvContractCompat.Channels._ID,
                        TvContractCompat.Channels.COLUMN_PACKAGE_NAME,
                        TvContractCompat.Channels.COLUMN_DISPLAY_NAME
                    )

                    contentResolver.query(
                        TvContractCompat.Channels.CONTENT_URI,
                        projection,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        val idCol = cursor.getColumnIndex(TvContractCompat.Channels._ID)
                        val pkgCol = cursor.getColumnIndex(TvContractCompat.Channels.COLUMN_PACKAGE_NAME)
                        val nameCol = cursor.getColumnIndex(TvContractCompat.Channels.COLUMN_DISPLAY_NAME)

                        while (cursor.moveToNext()) {
                            val channelId = cursor.getLong(idCol)
                            val pkgName = cursor.getString(pkgCol) ?: ""
                            val title = cursor.getString(nameCol) ?: ""

                            val appName = try {
                                val appInfo = packageManager.getApplicationInfo(pkgName, 0)
                                packageManager.getApplicationLabel(appInfo).toString()
                            } catch (e: PackageManager.NameNotFoundException) {
                                pkgName
                            }

                            val programs = loadProgramsForChannel(channelId)
                            if (programs.isNotEmpty()) {
                                channelList.add(
                                    ChannelModel(
                                        id = channelId,
                                        packageName = pkgName,
                                        title = if (title.isBlank()) appName else title,
                                        appName = appName,
                                        programs = programs
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("ChannelRepo", "System channels query exception: ${e.message}")
                }
            }

            // 3. Built-in preview channel swimlanes for installed TV apps
            val appChannels = buildAppChannels(packageManager)
            for (ch in appChannels) {
                if (channelList.none { it.id == ch.id }) {
                    channelList.add(ch)
                }
            }

            channelsStateFlow.value = channelList
        }
    }

    private fun loadWatchNextPrograms(): List<ProgramModel> {
        val programs = mutableListOf<ProgramModel>()
        try {
            val projection = arrayOf(
                TvContractCompat.WatchNextPrograms._ID,
                TvContractCompat.WatchNextPrograms.COLUMN_PACKAGE_NAME,
                TvContractCompat.WatchNextPrograms.COLUMN_TITLE,
                TvContractCompat.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION,
                TvContractCompat.WatchNextPrograms.COLUMN_POSTER_ART_URI,
                TvContractCompat.WatchNextPrograms.COLUMN_INTENT_URI
            )
            context.contentResolver.query(
                TvContractCompat.WatchNextPrograms.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms._ID)
                val titleCol = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_TITLE)
                val descCol = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_SHORT_DESCRIPTION)
                val posterCol = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_POSTER_ART_URI)
                val intentCol = cursor.getColumnIndex(TvContractCompat.WatchNextPrograms.COLUMN_INTENT_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: ""
                    val desc = cursor.getString(descCol)
                    val posterStr = cursor.getString(posterCol)
                    val intentStr = cursor.getString(intentCol)
                    programs.add(
                        ProgramModel(
                            id = id,
                            channelId = -100L,
                            title = title,
                            description = desc,
                            posterArtUri = if (!posterStr.isNullOrBlank()) Uri.parse(posterStr) else null,
                            intentUri = if (!intentStr.isNullOrBlank()) Uri.parse(intentStr) else null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("ChannelRepo", "WatchNext query exception: ${e.message}")
        }
        return programs
    }

    private fun buildAppChannels(packageManager: PackageManager): List<ChannelModel> {
        val list = mutableListOf<ChannelModel>()

        // 1. SmartTube Channels
        try {
            packageManager.getPackageInfo("org.smarttube.stable", 0)
            list.add(
                ChannelModel(
                    id = 1001L,
                    packageName = "org.smarttube.stable",
                    title = "טרנדים ומוזיקה",
                    appName = "SmartTube",
                    programs = fetchSmartTubeTrending()
                )
            )
            list.add(
                ChannelModel(
                    id = 1002L,
                    packageName = "org.smarttube.stable",
                    title = "להיטים עולמיים",
                    appName = "SmartTube",
                    programs = fetchSmartTubeHits()
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Not installed
        }

        // 2. Stremio Channels
        try {
            packageManager.getPackageInfo("com.stremio.one", 0)
            val launchIntent = packageManager.getLeanbackLaunchIntentForPackage("com.stremio.one")
                ?: packageManager.getLaunchIntentForPackage("com.stremio.one")
            val intentUri = launchIntent?.toUri(Intent.URI_INTENT_SCHEME)?.let { Uri.parse(it) }

            list.add(
                ChannelModel(
                    id = 1011L,
                    packageName = "com.stremio.one",
                    title = "סרטים פופולריים",
                    appName = "Stremio",
                    programs = fetchStremioMovies(intentUri)
                )
            )
            list.add(
                ChannelModel(
                    id = 1012L,
                    packageName = "com.stremio.one",
                    title = "סדרות מובילות",
                    appName = "Stremio",
                    programs = fetchStremioSeries()
                )
            )
            list.add(
                ChannelModel(
                    id = 1013L,
                    packageName = "com.stremio.one",
                    title = "קולנוע מובחר",
                    appName = "Stremio",
                    programs = fetchStremioClassics()
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Not installed
        }

        // 3. Spotify TV Channel
        try {
            packageManager.getPackageInfo("com.spotify.tv.android", 0)
            list.add(
                ChannelModel(
                    id = 1021L,
                    packageName = "com.spotify.tv.android",
                    title = "פלייליסטים מובילים",
                    appName = "Spotify",
                    programs = fetchSpotifyPlaylists()
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // Not installed
        }

        return list
    }

    private fun fetchSmartTubeTrending(): List<ProgramModel> {
        val trendingVideos = listOf(
            Triple("k2qgadSvNyU", "Dua Lipa - Training Season", "https://i.ytimg.com/vi/k2qgadSvNyU/hqdefault.jpg"),
            Triple("2Vv-BfVoq4g", "Ed Sheeran - Perfect", "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg"),
            Triple("kJQP7kiw5Fk", "Luis Fonsi - Despacito", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg"),
            Triple("fJ9rUzIMcZQ", "Queen - Bohemian Rhapsody", "https://i.ytimg.com/vi/fJ9rUzIMcZQ/hqdefault.jpg"),
            Triple("JGwWNGJdvx8", "Ed Sheeran - Shape of You", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg"),
            Triple("hT_nvWreIhg", "OneRepublic - Counting Stars", "https://i.ytimg.com/vi/hT_nvWreIhg/hqdefault.jpg")
        )
        return trendingVideos.mapIndexed { index, (vId, title, thumb) ->
            val videoIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=vnd.youtube%3A$vId;package=org.smarttube.stable;end")
            ProgramModel(
                id = 10010L + index,
                channelId = 1001L,
                title = title,
                description = "Watch on SmartTube",
                posterArtUri = Uri.parse(thumb),
                intentUri = videoIntentUri
            )
        }
    }

    private fun fetchSmartTubeHits(): List<ProgramModel> {
        val hits = listOf(
            Triple("09R8_2nJtjg", "Maroon 5 - Sugar", "https://i.ytimg.com/vi/09R8_2nJtjg/hqdefault.jpg"),
            Triple("OPf0YbXqDm0", "Mark Ronson - Uptown Funk", "https://i.ytimg.com/vi/OPf0YbXqDm0/hqdefault.jpg"),
            Triple("YQHsXMglC9A", "Adele - Hello", "https://i.ytimg.com/vi/YQHsXMglC9A/hqdefault.jpg"),
            Triple("CevxZvSJLk8", "Katy Perry - Roar", "https://i.ytimg.com/vi/CevxZvSJLk8/hqdefault.jpg"),
            Triple("2Vv-BfVoq4g", "Ed Sheeran - Perfect", "https://i.ytimg.com/vi/2Vv-BfVoq4g/hqdefault.jpg"),
            Triple("kJQP7kiw5Fk", "Luis Fonsi - Despacito", "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg")
        )
        return hits.mapIndexed { index, (vId, title, thumb) ->
            val videoIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=vnd.youtube%3A$vId;package=org.smarttube.stable;end")
            ProgramModel(
                id = 10020L + index,
                channelId = 1002L,
                title = title,
                description = "Play on SmartTube",
                posterArtUri = Uri.parse(thumb),
                intentUri = videoIntentUri
            )
        }
    }

    private fun fetchStremioMovies(fallbackIntentUri: Uri?): List<ProgramModel> {
        val programs = mutableListOf<ProgramModel>()
        try {
            val url = java.net.URL("https://cinemeta-catalogs.strem.io/top/catalog/movie/top.json")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "MinimalTV/1.0")
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val rootJson = org.json.JSONObject(responseText)
                val metas = rootJson.optJSONArray("metas")
                if (metas != null) {
                    for (i in 0 until minOf(metas.length(), 10)) {
                        val item = metas.getJSONObject(i)
                        val imdbId = item.optString("imdb_id")
                        val name = item.optString("name")
                        val desc = item.optString("description")
                        val posterUrl = item.optString("poster")

                        val movieIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=stremio%3A%2F%2F%2Fdetail%2Fmovie%2F$imdbId;package=com.stremio.one;end")
                        programs.add(
                            ProgramModel(
                                id = 10110L + i,
                                channelId = 1011L,
                                title = name,
                                description = desc,
                                posterArtUri = if (posterUrl.isNotEmpty()) Uri.parse(posterUrl) else null,
                                intentUri = movieIntentUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("ChannelRepo", "Cinemeta fetch error: ${e.message}")
        }

        if (programs.isEmpty()) {
            val fallbacks = listOf(
                Triple("tt15239678", "Dune: Part Two", "https://images.metahub.space/poster/small/tt15239678/img"),
                Triple("tt6262770", "The Flash", "https://images.metahub.space/poster/small/tt6262770/img"),
                Triple("tt27369017", "Rebel Ridge", "https://images.metahub.space/poster/small/tt27369017/img"),
                Triple("tt22084616", "Civil War", "https://images.metahub.space/poster/small/tt22084616/img"),
                Triple("tt15398776", "Oppenheimer", "https://images.metahub.space/poster/small/tt15398776/img")
            )
            for ((idx, fb) in fallbacks.withIndex()) {
                val (id, title, poster) = fb
                val movieIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=stremio%3A%2F%2F%2Fdetail%2Fmovie%2F$id;package=com.stremio.one;end")
                programs.add(
                    ProgramModel(
                        id = 10110L + idx,
                        channelId = 1011L,
                        title = title,
                        description = "Play on Stremio",
                        posterArtUri = Uri.parse(poster),
                        intentUri = movieIntentUri
                    )
                )
            }
        }
        return programs
    }

    private fun fetchStremioSeries(): List<ProgramModel> {
        val series = listOf(
            Triple("tt12637874", "Fallout", "https://images.metahub.space/poster/small/tt12637874/img"),
            Triple("tt2788316", "Shōgun", "https://images.metahub.space/poster/small/tt2788316/img"),
            Triple("tt4574334", "Stranger Things", "https://images.metahub.space/poster/small/tt4574334/img"),
            Triple("tt11198330", "House of the Dragon", "https://images.metahub.space/poster/small/tt11198330/img"),
            Triple("tt0903747", "Breaking Bad", "https://images.metahub.space/poster/small/tt0903747/img"),
            Triple("tt0944947", "Game of Thrones", "https://images.metahub.space/poster/small/tt0944947/img")
        )
        return series.mapIndexed { index, (id, title, poster) ->
            val seriesIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=stremio%3A%2F%2F%2Fdetail%2Fseries%2F$id;package=com.stremio.one;end")
            ProgramModel(
                id = 10120L + index,
                channelId = 1012L,
                title = title,
                description = "Watch Series on Stremio",
                posterArtUri = Uri.parse(poster),
                intentUri = seriesIntentUri
            )
        }
    }

    private fun fetchStremioClassics(): List<ProgramModel> {
        val classics = listOf(
            Triple("tt0816692", "Interstellar", "https://images.metahub.space/poster/small/tt0816692/img"),
            Triple("tt1375666", "Inception", "https://images.metahub.space/poster/small/tt1375666/img"),
            Triple("tt0468569", "The Dark Knight", "https://images.metahub.space/poster/small/tt0468569/img"),
            Triple("tt15239678", "Dune: Part Two", "https://images.metahub.space/poster/small/tt15239678/img"),
            Triple("tt15398776", "Oppenheimer", "https://images.metahub.space/poster/small/tt15398776/img")
        )
        return classics.mapIndexed { index, (id, title, poster) ->
            val movieIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=stremio%3A%2F%2F%2Fdetail%2Fmovie%2F$id;package=com.stremio.one;end")
            ProgramModel(
                id = 10130L + index,
                channelId = 1013L,
                title = title,
                description = "Play Cinema on Stremio",
                posterArtUri = Uri.parse(poster),
                intentUri = movieIntentUri
            )
        }
    }

    private fun fetchSpotifyPlaylists(): List<ProgramModel> {
        val tracks = listOf(
            Triple("spotify:playlist:37i9dQZF1DX4sWSpwq3LiO", "Peaceful Piano", "https://i.scdn.co/image/ab67706f00000002ca5a7517156021292e5663a6"),
            Triple("spotify:playlist:37i9dQZF1DWZeKCadgRdKQ", "Deep Focus", "https://i.scdn.co/image/ab67706f00000002d073e656e546e43bc387ad79"),
            Triple("spotify:track:7qiZfU4dY1lWllzX7mPBI3", "Ed Sheeran - Shape of You", "https://i.scdn.co/image/ab67616d0000b273ba5db46f4b838ef6027e6f96"),
            Triple("spotify:track:0VjIjW4GlUZAMYd2vXMi3b", "The Weeknd - Blinding Lights", "https://i.scdn.co/image/ab67616d0000b273bb54dde68cd23e2a268ae0f5")
        )
        return tracks.mapIndexed { index, (uriStr, title, poster) ->
            val spotifyIntentUri = Uri.parse("intent:#Intent;action=android.intent.action.VIEW;data=${Uri.encode(uriStr)};package=com.spotify.tv.android;end")
            ProgramModel(
                id = 10210L + index,
                channelId = 1021L,
                title = title,
                description = "Play on Spotify",
                posterArtUri = Uri.parse(poster),
                intentUri = spotifyIntentUri
            )
        }
    }

    private fun loadProgramsForChannel(channelId: Long): List<ProgramModel> {
        val programs = mutableListOf<ProgramModel>()
        try {
            val channelProgramsUri = TvContractCompat.buildPreviewProgramsUriForChannel(channelId)
            context.contentResolver.query(
                channelProgramsUri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms._ID)
                val titleCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_TITLE)
                val descCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_SHORT_DESCRIPTION)
                val posterCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI)
                val intentCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI)

                while (cursor.moveToNext()) {
                    val id = if (idCol != -1) cursor.getLong(idCol) else 0L
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "" else ""
                    val desc = if (descCol != -1) cursor.getString(descCol) else null
                    val posterStr = if (posterCol != -1) cursor.getString(posterCol) else null
                    val intentStr = if (intentCol != -1) cursor.getString(intentCol) else null

                    val posterUri = if (!posterStr.isNullOrBlank()) Uri.parse(posterStr) else null
                    val intentUri = if (!intentStr.isNullOrBlank()) Uri.parse(intentStr) else null

                    programs.add(
                        ProgramModel(
                            id = id,
                            channelId = channelId,
                            title = title,
                            description = desc,
                            posterArtUri = posterUri,
                            intentUri = intentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("ChannelRepo", "loadProgramsForChannel error: ${e.message}")
        }
        return programs
    }

    override suspend fun setChannelEnabled(channelId: Long, enabled: Boolean) {
        preferencesRepository.setChannelEnabled(channelId, enabled)
    }

    /**
     * Moves a channel one step up/down relative to the other *enabled* channels (the ones shown on the home screen),
     * so a single D-pad press always produces a visible change. Disabled channels keep their relative slots.
     */
    override suspend fun moveChannel(channelId: Long, direction: Int) {
        moveMutex.withLock {
            withContext(Dispatchers.IO) {
                val sorted = getPreviewChannels().first()
                val order = sorted.map { it.id.toString() }.toMutableList()
                val enabledIds = sorted.filter { it.isEnabled }.map { it.id.toString() }
                val currentEnabledIndex = enabledIds.indexOf(channelId.toString())
                if (currentEnabledIndex == -1) return@withContext
                val neighborId = enabledIds.getOrNull(currentEnabledIndex + direction) ?: return@withContext
                val from = order.indexOf(channelId.toString())
                val to = order.indexOf(neighborId)
                if (from == -1 || to == -1) return@withContext
                order[from] = neighborId
                order[to] = channelId.toString()
                preferencesRepository.saveChannelOrder(order)
            }
        }
    }
}
