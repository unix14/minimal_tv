package com.unix14.minimaltv.ui.presenter

import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.RowPresenter

class CustomListRowPresenter : ListRowPresenter() {
    init {
        // Optimize for 60fps on budget TV hardware: disable software shadow overhead
        shadowEnabled = false
        selectEffectEnabled = false
    }

    override fun isUsingDefaultListSelectEffect(): Boolean {
        return false
    }
}
