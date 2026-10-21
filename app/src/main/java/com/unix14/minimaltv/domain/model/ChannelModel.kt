package com.unix14.minimaltv.domain.model

data class ChannelModel(
    val id: Long,
    val packageName: String,
    val title: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val programs: List<ProgramModel> = emptyList()
)
