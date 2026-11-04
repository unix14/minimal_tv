package com.unix14.minimaltv.domain.model

import com.unix14.minimaltv.R

data class HdmiCardItem(
    val id: String = "ps5",
    val title: String = "PlayStation 5",
    val subtitle: String = "HDMI 2",
    val inputId: String = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW3",
    val portId: Int = 2,
    val iconRes: Int = R.drawable.ic_ps5
) {
    val orderKey: String get() = HOME_KEY_PREFIX + id

    companion object {
        const val HOME_KEY_PREFIX = "hdmi:"
        const val ID_PS5 = "ps5"
        const val ID_XBOX_PC = "xbox_pc"

        fun defaults(): List<HdmiCardItem> = listOf(
            HdmiCardItem(
                id = ID_PS5,
                title = "PlayStation 5",
                subtitle = "HDMI 2",
                inputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW3",
                portId = 2,
                iconRes = R.drawable.ic_ps5
            ),
            HdmiCardItem(
                id = ID_XBOX_PC,
                title = "Xbox / PC",
                subtitle = "HDMI 1",
                inputId = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW2",
                portId = 1,
                iconRes = R.drawable.ic_xbox_pc
            )
        )
    }
}
