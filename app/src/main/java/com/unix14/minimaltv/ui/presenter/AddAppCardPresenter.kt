package com.unix14.minimaltv.ui.presenter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import com.unix14.minimaltv.R

class AddAppCardPresenter(
    private val onAddClick: () -> Unit
) : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val root: View = view.findViewById(R.id.add_card_root)
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_add_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val holder = viewHolder as ViewHolder
        holder.root.isFocusable = true
        holder.root.isClickable = true

        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.05f, 10f)
            } else {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.0f, 2f)
            }
        }

        holder.root.setOnClickListener {
            onAddClick()
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.root.onFocusChangeListener = null
        holder.root.setOnClickListener(null)
    }
}
