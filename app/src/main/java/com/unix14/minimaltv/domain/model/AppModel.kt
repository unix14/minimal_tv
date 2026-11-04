package com.unix14.minimaltv.domain.model

import android.graphics.drawable.Drawable

data class AppModel(
    val packageName: String,
    val activityName: String,
    val label: String,
    val banner: Drawable?,
    val icon: Drawable?,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false
) {
    val isBanner: Boolean get() = banner != null
    val bannerOrIcon: Drawable? get() = banner ?: icon
}
