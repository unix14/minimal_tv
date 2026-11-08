package com.unix14.minimaltv.ui.presenter

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.HdmiCardItem

class HdmiCardPresenter(
    private val activeReorderKey: () -> String?,
    private val onMove: (String, Int) -> Unit,
    private val onFinishReorder: () -> Unit,
    private val onHdmiClick: (HdmiCardItem) -> Unit,
    private val onHdmiLongClick: (View, HdmiCardItem) -> Unit
) : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val root: View = view.findViewById(R.id.hdmi_card_root)
        val icon: android.widget.ImageView = view.findViewById(R.id.iv_hdmi_icon)
        val title: TextView = view.findViewById(R.id.tv_hdmi_title)
        val port: TextView = view.findViewById(R.id.tv_hdmi_port)
        val reorderOverlay: View = view.findViewById(R.id.layout_reorder_overlay)
        var isLongPressHandled: Boolean = false
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_hdmi_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val hdmi = item as? HdmiCardItem ?: return
        val holder = viewHolder as ViewHolder

        holder.icon.setImageResource(hdmi.iconRes)
        holder.title.text = hdmi.title
        holder.port.text = hdmi.subtitle

        holder.root.isFocusable = true
        holder.root.isClickable = true

        if (activeReorderKey() == hdmi.orderKey) {
            bindReorderMode(holder, hdmi)
            return
        }

        holder.reorderOverlay.visibility = View.GONE
        holder.root.isLongClickable = true
        holder.isLongPressHandled = false

        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.05f, 10f)
            } else {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.0f, 2f)
            }
        }

        // TV remote: holding D-Pad center opens the context popup, same as app cards
        holder.root.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_DOWN) {
                onHdmiLongClick(holder.root, hdmi)
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
                            onHdmiLongClick(holder.root, hdmi)
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
            onHdmiLongClick(holder.root, hdmi)
            true
        }

        holder.root.setOnClickListener {
            if (holder.isLongPressHandled) {
                holder.isLongPressHandled = false
                return@setOnClickListener
            }
            onHdmiClick(hdmi)
        }
    }

    private fun bindReorderMode(holder: ViewHolder, hdmi: HdmiCardItem) {
        holder.root.isFocusable = true
        holder.root.isFocusableInTouchMode = true
        holder.root.isClickable = true
        holder.reorderOverlay.visibility = View.VISIBLE
        holder.root.scaleX = 1.08f
        holder.root.scaleY = 1.08f
        com.unix14.minimaltv.util.Motion.elevation(holder.root, 16f)

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
                        onMove(hdmi.orderKey, if (isRtl) 1 else -1)
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        onMove(hdmi.orderKey, if (isRtl) -1 else 1)
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

        holder.root.setOnClickListener { onFinishReorder() }
        holder.root.setOnLongClickListener(null)
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.reorderOverlay.visibility = View.GONE
        holder.root.onFocusChangeListener = null
        holder.root.setOnKeyListener(null)
        holder.root.setOnClickListener(null)
        holder.root.setOnLongClickListener(null)
    }
}
