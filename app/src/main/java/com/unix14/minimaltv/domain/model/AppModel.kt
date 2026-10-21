package com.unix14.minimaltv.domain.model

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val bannerOrIcon: Drawable?,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val isBanner: Boolean = false
)
