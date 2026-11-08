package com.unix14.minimaltv.ui.presenter

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel

class AppCardPresenter(
    private val activeReorderKey: () -> String?,
    private val onMoveApp: (String, Int) -> Unit,
    private val onFinishReorder: () -> Unit,
    private val onAppLongClick: (View, AppModel) -> Unit
) : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val root: View = view.findViewById(R.id.app_card_root)
        val banner: ImageView = view.findViewById(R.id.iv_app_banner)
        val iconModeLayout: View = view.findViewById(R.id.layout_icon_mode)
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
        val label: TextView = view.findViewById(R.id.tv_app_label)
        val reorderOverlay: View = view.findViewById(R.id.layout_reorder_overlay)
        var isLongPressHandled: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_app_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val app = item as? AppModel ?: return
        val holder = viewHolder as ViewHolder

        if (app.banner != null) {
            holder.banner.visibility = View.VISIBLE
            holder.banner.setImageDrawable(app.banner)
            holder.iconModeLayout.visibility = View.GONE
        } else {
            holder.banner.visibility = View.GONE
            holder.banner.setImageDrawable(null)
            holder.iconModeLayout.visibility = View.VISIBLE
            holder.icon.setImageDrawable(app.icon ?: ContextCompat.getDrawable(holder.root.context, R.drawable.ic_tv))
            holder.label.text = app.label
        }

        val isReordering = activeReorderKey() == app.packageName
        holder.root.isFocusable = true
        holder.root.isFocusableInTouchMode = true
        holder.root.isClickable = true

        if (isReordering) {
            holder.reorderOverlay.visibility = View.VISIBLE
            holder.root.scaleX = 1.08f
            holder.root.scaleY = 1.08f
            com.unix14.minimaltv.util.Motion.elevation(holder.root, 16f)
            holder.label.setTextColor(Color.WHITE)

            holder.root.setOnFocusChangeListener { _, _ ->
                holder.root.scaleX = 1.08f
                holder.root.scaleY = 1.08f
                com.unix14.minimaltv.util.Motion.elevation(holder.root, 16f)
            }

            holder.root.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    val isRtl = holder.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val delta = if (isRtl) 1 else -1
                            onMoveApp(app.packageName, delta)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val delta = if (isRtl) -1 else 1
                            onMoveApp(app.packageName, delta)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> {
                            onFinishReorder()
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }

            holder.root.setOnClickListener {
                onFinishReorder()
            }
            holder.root.setOnLongClickListener(null)
            return
        }

        holder.reorderOverlay.visibility = View.GONE
        holder.root.isLongClickable = true
        holder.isLongPressHandled = false

        // Subtle 1.05x focus animation with soft elevation
        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.05f, 10f)
                holder.label.setTextColor(Color.WHITE)
            } else {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.0f, 2f)
                holder.label.setTextColor(ContextCompat.getColor(v.context, R.color.text_primary))
            }
        }

        // TV Remote D-Pad Center long-press tracking
        holder.root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
                onAppLongClick(holder.root, app)
                return@setOnKeyListener true
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        holder.isLongPressHandled = false
                        event.startTracking()
                    } else if (event.repeatCount >= 1 || event.isLongPress) {
                        if (!holder.isLongPressHandled) {
                            holder.isLongPressHandled = true
                            onAppLongClick(holder.root, app)
                        }
                        return@setOnKeyListener true
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    if (holder.isLongPressHandled) {
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }

        holder.root.setOnLongClickListener {
            holder.isLongPressHandled = true
            onAppLongClick(holder.root, app)
            true
        }

        holder.root.setOnClickListener {
            if (holder.isLongPressHandled) {
                holder.isLongPressHandled = false
                return@setOnClickListener
            }
            val context = holder.root.context
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                component = ComponentName(app.packageName, app.activityName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            }

            val opts = com.unix14.minimaltv.util.Motion.launchOptions(holder.root, 1.05f)

            try {
                context.startActivity(intent, opts)
            } catch (e: Exception) {
                val fallbackIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent, opts)
                }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.reorderOverlay.visibility = View.GONE
        holder.banner.setImageDrawable(null)
        holder.icon.setImageDrawable(null)
        holder.root.onFocusChangeListener = null
        holder.root.setOnKeyListener(null)
        holder.root.setOnClickListener(null)
        holder.root.setOnLongClickListener(null)
    }
}
