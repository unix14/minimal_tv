package com.unix14.minimaltv.ui.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.HdmiCardItem

/** Long-press popup for the PS5 / Xbox HDMI shortcuts: open the input or move it along the first swimlane. */
class HdmiContextPopupWindow(
    private val context: Context,
    private val hdmi: HdmiCardItem,
    private val onOpen: () -> Unit,
    private val onReorder: () -> Unit
) {
    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_app_context, null)

        view.findViewById<TextView>(R.id.tv_popup_app_name).text = hdmi.title
        val btnOpen = view.findViewById<View>(R.id.btn_popup_open)
        val btnReorder = view.findViewById<View>(R.id.btn_popup_reorder)

        listOf(
            R.id.btn_popup_favorite,
            R.id.btn_popup_hide,
            R.id.btn_popup_app_info,
            R.id.btn_popup_uninstall
        ).forEach { view.findViewById<View>(it).visibility = View.GONE }

        popupWindow = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 24f
            setOnDismissListener {
                anchorView.isFocusable = true
                anchorView.isFocusableInTouchMode = true
                anchorView.requestFocus()
            }
        }

        btnOpen.setOnClickListener {
            popupWindow?.dismiss()
            onOpen()
        }
        btnReorder.setOnClickListener {
            popupWindow?.setOnDismissListener(null)
            popupWindow?.dismiss()
            onReorder()
        }

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        if (location[1] > context.resources.displayMetrics.heightPixels / 2) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            popupWindow?.showAsDropDown(anchorView, 0, -(anchorView.height + view.measuredHeight + 10), Gravity.START)
        } else {
            popupWindow?.showAsDropDown(anchorView, 0, 10, Gravity.START)
        }
        view.post {
            btnOpen.requestFocus()
        }
    }
}
