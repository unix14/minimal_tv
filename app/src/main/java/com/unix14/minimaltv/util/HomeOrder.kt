package com.unix14.minimaltv.util

import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.HdmiCardItem

/**
 * Shared ordering logic for the first swimlane, which mixes favorite apps and HDMI shortcuts (PS5, Xbox / PC).
 * Apps are keyed by package name, HDMI shortcuts by [HdmiCardItem.orderKey].
 */
object HomeOrder {

    fun keyOf(item: Any): String? = when (item) {
        is AppModel -> item.packageName
        is HdmiCardItem -> item.orderKey
        else -> null
    }

    fun sort(apps: List<AppModel>, hdmiItems: List<HdmiCardItem>, savedOrder: List<String>): List<Any> {
        val orderMap = savedOrder.withIndex().associate { it.value to it.index }
        val base: List<Any> = apps + hdmiItems
        return base.withIndex()
            .sortedWith(compareBy({ orderMap[keyOf(it.value)] ?: Int.MAX_VALUE }, { it.index }))
            .map { it.value }
    }
}
