package com.unix14.minimaltv.domain.model

import android.net.Uri

data class ProgramModel(
    val id: Long,
    val channelId: Long,
    val title: String,
    val description: String?,
    val posterArtUri: Uri?,
    val intentUri: Uri?
)
