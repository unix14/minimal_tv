package com.unix14.minimaltv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.leanback.widget.VerticalGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.ui.main.MainViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddAppsDialogFragment : DialogFragment() {

    private lateinit var viewModel: MainViewModel

    companion object {
        fun newInstance(viewModel: MainViewModel): AddAppsDialogFragment {
            val fragment = AddAppsDialogFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (860 * resources.displayMetrics.density).toInt()
            val height = (560 * resources.displayMetrics.density).toInt()
            setLayout(width, height)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_add_apps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnDone = view.findViewById<Button>(R.id.btn_done_picker)
        val grid = view.findViewById<VerticalGridView>(R.id.grid_all_apps)

        grid.setNumColumns(3)
        val adapter = AppPickerAdapter(
            onToggle = { app, isFav ->
                if (isFav) {
                    viewModel.unpinFavorite(app.packageName)
                } else {
                    viewModel.pinFavorite(app.packageName)
                }
            }
        )
        grid.adapter = adapter

        btnDone.setOnClickListener {
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.installedApps.collectLatest { apps ->
                    adapter.submitList(apps)
                }
            }
        }
    }

    private class AppPickerAdapter(
        private val onToggle: (AppModel, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppPickerAdapter.PickerViewHolder>() {

        private var items: List<AppModel> = emptyList()

        fun submitList(newItems: List<AppModel>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PickerViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_app_picker, parent, false)
            return PickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PickerViewHolder, position: Int) {
            val app = items[position]
            holder.bind(app, onToggle)
        }

        override fun getItemCount(): Int = items.size

        class PickerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val icon: ImageView = view.findViewById(R.id.iv_picker_icon)
            private val label: TextView = view.findViewById(R.id.tv_picker_label)
            private val checkLayout: FrameLayout = view.findViewById(R.id.layout_picker_check)
            private val checkIcon: ImageView = view.findViewById(R.id.iv_picker_check)

            fun bind(app: AppModel, onToggle: (AppModel, Boolean) -> Unit) {
                label.text = app.label
                icon.setImageDrawable(app.icon ?: ContextCompat.getDrawable(itemView.context, R.drawable.ic_tv))

                updateCheckState(app.isFavorite)

                itemView.setOnClickListener {
                    val nextState = !app.isFavorite
                    updateCheckState(nextState)
                    onToggle(app, app.isFavorite)
                }
            }

            private fun updateCheckState(isFavorite: Boolean) {
                val circle = ContextCompat.getDrawable(itemView.context, R.drawable.bg_badge_circle)?.mutate() as? GradientDrawable
                if (isFavorite) {
                    circle?.setColor(ContextCompat.getColor(itemView.context, R.color.accent))
                    checkLayout.background = circle
                    checkIcon.visibility = View.VISIBLE
                } else {
                    circle?.setColor(Color.parseColor("#27272A"))
                    checkLayout.background = circle
                    checkIcon.visibility = View.GONE
                }
            }
        }
    }
}
