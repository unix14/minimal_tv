package com.unix14.minimaltv.ui.presenter

import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.leanback.widget.BaseGridView
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ListRowView
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter
import androidx.leanback.widget.RowPresenter
import com.unix14.minimaltv.R

class CustomListRowPresenter : ListRowPresenter() {

    init {
        shadowEnabled = false
        selectEffectEnabled = false
        headerPresenter = CustomRowHeaderPresenter()
    }

    override fun isUsingDefaultListSelectEffect(): Boolean = false

    override fun createRowViewHolder(parent: ViewGroup): RowPresenter.ViewHolder {
        val rowView = ListRowView(parent.context)
        val overscan = parent.context.resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        val hgv = rowView.gridView
        hgv.setPaddingRelative(overscan, 0, overscan, 0)
        hgv.clipToPadding = false
        return ViewHolder(rowView, hgv, this)
    }

    override fun initializeRowViewHolder(holder: RowPresenter.ViewHolder) {
        super.initializeRowViewHolder(holder)
        val rowViewHolder = holder as? ListRowPresenter.ViewHolder ?: return
        val hgv = rowViewHolder.gridView
        val overscan = hgv.resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
        hgv.setPaddingRelative(overscan, 0, overscan, 0)
        hgv.clipToPadding = false
        hgv.windowAlignment = BaseGridView.WINDOW_ALIGN_BOTH_EDGE
        hgv.windowAlignmentOffset = overscan
        hgv.windowAlignmentOffsetPercent = HorizontalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED
        hgv.itemAlignmentOffset = 0
        hgv.itemAlignmentOffsetPercent = HorizontalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED
    }

    private class CustomRowHeaderPresenter : RowHeaderPresenter() {
        override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
            super.onBindViewHolder(viewHolder, item)
            val v = viewHolder.view
            val headerPadding = v.resources.getDimensionPixelSize(R.dimen.header_padding_horizontal)
            val overscan = v.resources.getDimensionPixelSize(R.dimen.tv_overscan_horizontal)
            v.setPaddingRelative(headerPadding, v.paddingTop, overscan, v.paddingBottom)

            val headerView = (v as? TextView) ?: v.findViewById<TextView>(androidx.leanback.R.id.row_header)
            headerView?.apply {
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                alpha = 0.9f
            }
        }
    }
}
