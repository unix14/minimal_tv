package com.unix14.minimaltv.ui.dialog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.ui.main.MainViewModel

class AppContextPopupWindow(
    private val context: Context,
    private val app: AppModel,
    private val viewModel: MainViewModel
) {
    private var popupWindow: PopupWindow? = null

    fun show(anchorView: View) {
        val view = LayoutInflater.from(context).inflate(R.layout.popup_app_context, null)

        val tvAppName = view.findViewById<TextView>(R.id.tv_popup_app_name)
        val btnOpen = view.findViewById<View>(R.id.btn_popup_open)
        val btnReorder = view.findViewById<View>(R.id.btn_popup_reorder)
        val btnFavorite = view.findViewById<View>(R.id.btn_popup_favorite)
        val ivFavorite = view.findViewById<ImageView>(R.id.iv_popup_favorite)
        val tvFavorite = view.findViewById<TextView>(R.id.tv_popup_favorite)
        val btnHide = view.findViewById<View>(R.id.btn_popup_hide)
        val btnAppInfo = view.findViewById<View>(R.id.btn_popup_app_info)
        val btnUninstall = view.findViewById<View>(R.id.btn_popup_uninstall)

        tvAppName.text = app.label

        if (app.isFavorite) {
            ivFavorite.setImageResource(R.drawable.ic_unfavorite)
            tvFavorite.setText(R.string.action_unpin_favorite)
        } else {
            ivFavorite.setImageResource(R.drawable.ic_favorite)
            tvFavorite.setText(R.string.action_pin_favorite)
        }

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
            dismiss()
            launchApp()
        }

        btnReorder.setOnClickListener {
            popupWindow?.setOnDismissListener(null)
            dismiss()
            (context as? com.unix14.minimaltv.ui.main.MainActivity)?.startInPlaceReorder(app.packageName)
        }

        btnFavorite.setOnClickListener {
            dismiss()
            if (app.isFavorite) {
                viewModel.unpinFavorite(app.packageName)
            } else {
                viewModel.pinFavorite(app.packageName)
            }
        }

        btnHide.setOnClickListener {
            dismiss()
            viewModel.hideApp(app.packageName)
        }

        btnAppInfo.setOnClickListener {
            dismiss()
            openAppInfo()
        }

        btnUninstall.setOnClickListener {
            dismiss()
            uninstallApp()
        }

        val location = IntArray(2)
        anchorView.getLocationOnScreen(location)
        val anchorY = location[1]
        val displayHeight = context.resources.displayMetrics.heightPixels

        // If card is low on screen, show popup above the card
        if (anchorY > displayHeight / 2) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val popupHeight = view.measuredHeight
            popupWindow?.showAsDropDown(anchorView, 0, -(anchorView.height + popupHeight + 10), Gravity.START)
        } else {
            popupWindow?.showAsDropDown(anchorView, 0, 10, Gravity.START)
        }

        view.post {
            btnOpen.requestFocus()
        }
    }

    private fun dismiss() {
        popupWindow?.dismiss()
    }

    private fun launchApp() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(app.packageName, app.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallback = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (fallback != null) {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallback)
            }
        }
    }

    private fun openAppInfo() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun uninstallApp() {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.fromParts("package", app.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
