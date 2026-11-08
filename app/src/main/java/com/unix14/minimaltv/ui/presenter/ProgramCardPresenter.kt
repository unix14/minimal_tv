package com.unix14.minimaltv.ui.presenter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import coil.dispose
import coil.load
import coil.size.Scale
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.ProgramModel

class ProgramCardPresenter : Presenter() {

    class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        val root: View = view.findViewById(R.id.program_card_root)
        val poster: ImageView = view.findViewById(R.id.iv_program_poster)
        val title: TextView = view.findViewById(R.id.tv_program_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_program_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any) {
        val program = item as? ProgramModel ?: return
        val holder = viewHolder as ViewHolder

        holder.title.text = program.title
        holder.poster.setPadding(0, 0, 0, 0)
        holder.poster.scaleType = ImageView.ScaleType.CENTER_CROP

        if (program.posterArtUri != null) {
            holder.poster.load(program.posterArtUri) {
                crossfade(false)
                scale(Scale.FILL)
                size(coil.size.ViewSizeResolver(holder.poster))
                listener(
                    onError = { _, _ ->
                        loadFallbackAppVisual(holder, program)
                    }
                )
            }
        } else {
            loadFallbackAppVisual(holder, program)
        }

        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.06f, 10f)
            } else {
                com.unix14.minimaltv.util.Motion.focusCard(v, 1.0f, 2f)
            }
        }

        holder.root.setOnClickListener {
            val context = holder.root.context
            if (program.intentUri != null) {
                val opts = com.unix14.minimaltv.util.Motion.launchOptions(holder.root, 1.06f)

                try {
                    val intent = Intent.parseUri(program.intentUri.toString(), Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent, opts)
                } catch (e: Exception) {
                    // Fallback to app launch if direct intent fails
                }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.poster.dispose()
        holder.poster.setImageDrawable(null)
        holder.root.onFocusChangeListener = null
        holder.root.setOnClickListener(null)
    }

    private fun loadFallbackAppVisual(holder: ViewHolder, program: ProgramModel) {
        val uri = program.intentUri ?: return
        try {
            val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            val pkg = intent.`package` ?: intent.component?.packageName
            if (pkg != null) {
                val pm = holder.root.context.packageManager
                val banner = pm.getApplicationBanner(pkg)
                if (banner != null) {
                    holder.poster.setImageDrawable(banner)
                    holder.poster.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    val icon = pm.getApplicationIcon(pkg)
                    holder.poster.setImageDrawable(icon)
                    holder.poster.scaleType = ImageView.ScaleType.FIT_CENTER
                    holder.poster.setPadding(28, 16, 28, 32)
                }
            }
        } catch (e: Exception) {
            holder.poster.setImageDrawable(null)
        }
    }
}
