package com.unix14.minimaltv.ui.presenter

import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.leanback.widget.Presenter
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.ProgramModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        holder.poster.setImageDrawable(null)

        if (program.posterArtUri != null) {
            val uri = program.posterArtUri
            CoroutineScope(Dispatchers.Main).launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        holder.root.context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    holder.poster.setImageBitmap(bitmap)
                } else {
                    holder.poster.setImageResource(R.drawable.ic_tv)
                }
            }
        } else {
            holder.poster.setImageResource(R.drawable.ic_tv)
        }

        holder.root.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start()
                ViewCompat.setElevation(v, 10f)
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                ViewCompat.setElevation(v, 2f)
            }
        }

        holder.root.setOnClickListener {
            val context = holder.root.context
            if (program.intentUri != null) {
                try {
                    val intent = Intent.parseUri(program.intentUri.toString(), Intent.URI_INTENT_SCHEME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to app launch if direct intent fails
                }
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val holder = viewHolder as ViewHolder
        holder.poster.setImageDrawable(null)
        holder.root.onFocusChangeListener = null
        holder.root.setOnClickListener(null)
    }
}
