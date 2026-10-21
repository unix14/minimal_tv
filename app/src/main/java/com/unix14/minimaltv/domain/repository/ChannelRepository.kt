package com.unix14.minimaltv.domain.repository

import com.unix14.minimaltv.domain.model.ChannelModel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getPreviewChannels(): Flow<List<ChannelModel>>
    suspend fun refreshChannels()
    suspend fun setChannelEnabled(channelId: Long, enabled: Boolean)
}
