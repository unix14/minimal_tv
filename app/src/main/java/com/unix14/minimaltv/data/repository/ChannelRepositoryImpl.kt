package com.unix14.minimaltv.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.TvContractCompat
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.domain.model.ProgramModel
import com.unix14.minimaltv.domain.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val context: Context,
    private val preferencesRepository: PreferencesRepository
) : ChannelRepository {

    private val channelsStateFlow = MutableStateFlow<List<ChannelModel>>(emptyList())

    override fun getPreviewChannels(): Flow<List<ChannelModel>> {
        return combine(channelsStateFlow, preferencesRepository.disabledChannelsFlow) { channels, disabledIds ->
            channels.map { channel ->
                channel.copy(isEnabled = !disabledIds.contains(channel.id.toString()))
            }
        }
    }

    override suspend fun refreshChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelsStateFlow.value = emptyList()
            return
        }

        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val packageManager = context.packageManager
            val channelList = mutableListOf<ChannelModel>()

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
                // TV Provider may be unavailable, permission denied, or vendor-disabled
            }

            channelsStateFlow.value = channelList
        }
    }

    private fun loadProgramsForChannel(channelId: Long): List<ProgramModel> {
        val programs = mutableListOf<ProgramModel>()
        try {
            val projection = arrayOf(
                TvContractCompat.PreviewPrograms._ID,
                TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID,
                TvContractCompat.PreviewPrograms.COLUMN_TITLE,
                TvContractCompat.PreviewPrograms.COLUMN_SHORT_DESCRIPTION,
                TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI,
                TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI
            )

            val selection = "${TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID} = ?"
            val selectionArgs = arrayOf(channelId.toString())

            context.contentResolver.query(
                TvContractCompat.PreviewPrograms.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${TvContractCompat.PreviewPrograms.COLUMN_WEIGHT} DESC LIMIT 30"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms._ID)
                val chCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_CHANNEL_ID)
                val titleCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_TITLE)
                val descCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_SHORT_DESCRIPTION)
                val posterCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_POSTER_ART_URI)
                val intentCol = cursor.getColumnIndex(TvContractCompat.PreviewPrograms.COLUMN_INTENT_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val chId = cursor.getLong(chCol)
                    val title = cursor.getString(titleCol) ?: ""
                    val desc = cursor.getString(descCol)
                    val posterStr = cursor.getString(posterCol)
                    val intentStr = cursor.getString(intentCol)

                    val posterUri = if (!posterStr.isNullOrBlank()) Uri.parse(posterStr) else null
                    val intentUri = if (!intentStr.isNullOrBlank()) Uri.parse(intentStr) else null

                    programs.add(
                        ProgramModel(
                            id = id,
                            channelId = chId,
                            title = title,
                            description = desc,
                            posterArtUri = posterUri,
                            intentUri = intentUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore error per channel
        }
        return programs
    }

    override suspend fun setChannelEnabled(channelId: Long, enabled: Boolean) {
        preferencesRepository.setChannelEnabled(channelId, enabled)
    }
}
