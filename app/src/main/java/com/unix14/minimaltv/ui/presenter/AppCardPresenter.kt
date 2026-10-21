package com.unix14.minimaltv.ui.presenter

import android.content.ComponentName
import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel

class AppCardPresenter(
    private val onAppLongClick: (AppModel) -> Unit
) : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val root: View = view.findViewById(R.id.app_card_root)
        val icon: ImageView = view.findViewById(R.id.iv_app_icon)
        val label: TextView = view.findViewById(R.id.tv_app_label)
        val favoriteBadge: ImageView = view.findViewById(R.id.iv_favorite_badge)
        var isLongPressHandled: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_app_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val app = item as? AppModel ?: return
        val holder = viewHolder as ViewHolder

        holder.label.text = app.label
        if (app.bannerOrIcon != null) {
            holder.icon.setImageDrawable(app.bannerOrIcon)
        } else {
            holder.icon.setImageResource(R.drawable.ic_tv)
        }

        holder.favoriteBadge.visibility = if (app.isFavorite) View.VISIBLE else View.GONE

        holder.root.isFocusable = true
        holder.root.isClickable = true
        holder.root.isLongClickable = true
        holder.isLongPressHandled = false

        // Custom focus animation: smooth 1.08x scale up with elevation change
        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(150).start()
                ViewCompat.setElevation(v, 12f)
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                ViewCompat.setElevation(v, 2f)
            }
        }

        // TV Remote D-Pad Center long-press tracking
        holder.root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (event.repeatCount == 0) {
                        holder.isLongPressHandled = false
                        event.startTracking()
                    } else if (event.repeatCount >= 1 || event.isLongPress) {
                        if (!holder.isLongPressHandled) {
                            holder.isLongPressHandled = true
                            onAppLongClick(app)
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
            onAppLongClick(app)
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
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (fallbackIntent != null) {
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallbackIntent)
                }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.icon.setImageDrawable(null)
        holder.root.onFocusChangeListener = null
        holder.root.setOnKeyListener(null)
        holder.root.setOnClickListener(null)
        holder.root.setOnLongClickListener(null)
    }
}
