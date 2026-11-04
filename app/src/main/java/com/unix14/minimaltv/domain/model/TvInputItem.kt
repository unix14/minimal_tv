package com.unix14.minimaltv.domain.model

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import com.unix14.minimaltv.R

data class TvInputItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconRes: Int,
    val isTuner: Boolean,
    val inputId: String
)

object TvInputHelper {

    fun getAvailableInputs(context: Context): List<TvInputItem> {
        val list = mutableListOf<TvInputItem>()
        list.add(TvInputItem(id = "channels", title = context.getString(R.string.inputs_channels_title), subtitle = context.getString(R.string.inputs_channels_subtitle), iconRes = R.drawable.ic_tv, isTuner = true, inputId = "com.mediatek.tvinput/.tuner.TunerInputService/HW0"))
        list.add(TvInputItem(id = "hdmi3", title = "HDMI 3", subtitle = "HDMI 3", iconRes = R.drawable.ic_gamepad, isTuner = false, inputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW4"))
        list.add(TvInputItem(id = "ps5", title = "PlayStation 5", subtitle = "HDMI 2", iconRes = R.drawable.ic_ps5, isTuner = false, inputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW3"))
        list.add(TvInputItem(id = "xbox_pc", title = "Xbox / PC", subtitle = "HDMI 1", iconRes = R.drawable.ic_xbox_pc, isTuner = false, inputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW2"))
        list.add(TvInputItem(id = "av", title = "AV", subtitle = context.getString(R.string.inputs_composite_subtitle), iconRes = R.drawable.ic_tune, isTuner = false, inputId = "com.mediatek.tvinput/.composite.CompositeInputService/HW1"))
        return list
    }

    fun switchToInput(context: Context, item: TvInputItem) {
        try {
            val intent = if (item.isTuner) {
                Intent(Intent.ACTION_VIEW).apply {
                    data = TvContract.Channels.CONTENT_URI
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    data = TvContract.buildChannelUriForPassthroughInput(item.inputId)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = context.packageManager.getLaunchIntentForPackage("com.mediatek.wwtv.tvcenter")
                if (fallback != null) {
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                }
            } catch (ex: Exception) {
                android.util.Log.e("TvInputHelper", "Failed to switch to input ${item.inputId}", ex)
            }
        }
    }
}
