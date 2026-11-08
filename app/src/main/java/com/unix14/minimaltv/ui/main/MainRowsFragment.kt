package com.unix14.minimaltv.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ClassPresenterSelector
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unix14.minimaltv.MinimalTVApp
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AddCardItem
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.domain.model.HdmiCardItem
import com.unix14.minimaltv.ui.presenter.AddAppCardPresenter
import com.unix14.minimaltv.ui.presenter.AppCardPresenter
import com.unix14.minimaltv.ui.presenter.CustomListRowPresenter
import com.unix14.minimaltv.ui.presenter.HdmiCardPresenter
import com.unix14.minimaltv.ui.presenter.ProgramCardPresenter
import com.unix14.minimaltv.util.HomeOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainRowsFragment : RowsSupportFragment() {

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(
            MinimalTVApp.instance.appRepository,
            MinimalTVApp.instance.channelRepository,
            MinimalTVApp.instance.settingsRepository
        )
    }
    private val rowsAdapter = ArrayObjectAdapter(CustomListRowPresenter())
    private lateinit var mainSwimlaneAdapter: ArrayObjectAdapter

    private var currentFavorites: List<AppModel> = emptyList()
    private var currentChannels: List<ChannelModel> = emptyList()
    private var currentOrder: List<String> = emptyList()
    private var currentHomeItems: MutableList<Any> = mutableListOf()

    var activeReorderKey: String? = null
        private set

    val isInPlaceReordering: Boolean
        get() = activeReorderKey != null

    fun startInPlaceReorder(key: String) {
        val previousKey = activeReorderKey
        activeReorderKey = key
        if (previousKey != null && previousKey != key) {
            val prevIdx = currentHomeItems.indexOfFirst { HomeOrder.keyOf(it) == previousKey }
            if (prevIdx >= 0) mainSwimlaneAdapter.notifyArrayItemRangeChanged(prevIdx, 1)
        }
        val targetIndex = currentHomeItems.indexOfFirst { HomeOrder.keyOf(it) == key }
        if (targetIndex >= 0) {
            mainSwimlaneAdapter.notifyArrayItemRangeChanged(targetIndex, 1)
        }
        focusReorderItem(key)
    }

    fun finishInPlaceReorder() {
        val lastKey = activeReorderKey ?: return
        activeReorderKey = null
        val lastIdx = currentHomeItems.indexOfFirst { HomeOrder.keyOf(it) == lastKey }
        if (lastIdx >= 0) {
            mainSwimlaneAdapter.notifyArrayItemRangeChanged(lastIdx, 1)
            focusReorderItem(lastKey)
        }
    }

    fun moveItemInPlace(key: String, delta: Int) {
        val currentIndex = currentHomeItems.indexOfFirst { HomeOrder.keyOf(it) == key }
        if (currentIndex < 0) return
        val targetIndex = currentIndex + delta
        if (targetIndex !in 0 until currentHomeItems.size) return
        val item = currentHomeItems.removeAt(currentIndex)
        currentHomeItems.add(targetIndex, item)
        mainSwimlaneAdapter.move(currentIndex, targetIndex)
        viewModel.moveApp(key, delta)
        focusReorderItem(key)
    }

    fun firstReorderableKey(): String? = currentHomeItems.firstNotNullOfOrNull { HomeOrder.keyOf(it) }

    private fun focusReorderItem(key: String) {
        val targetIndex = currentHomeItems.indexOfFirst { HomeOrder.keyOf(it) == key }
        if (targetIndex < 0) return
        view?.post {
            val rowViewHolder = findRowViewHolderByPosition(0) as? androidx.leanback.widget.ListRowPresenter.ViewHolder
            val gridView = rowViewHolder?.gridView ?: return@post
            gridView.requestFocus()
            val task = androidx.leanback.widget.ViewHolderTask { viewHolder ->
                viewHolder?.itemView?.apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                    requestFocus()
                }
            }
            gridView.setSelectedPosition(targetIndex, task)
            val vh = gridView.findViewHolderForAdapterPosition(targetIndex)
            vh?.itemView?.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                requestFocus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenterSelector = ClassPresenterSelector().apply {
            addClassPresenter(AppModel::class.java, AppCardPresenter(
                activeReorderKey = { activeReorderKey },
                onMoveApp = { key, delta -> moveItemInPlace(key, delta) },
                onFinishReorder = { finishInPlaceReorder() },
                onAppLongClick = { anchorView, app ->
                    if (!isInPlaceReordering) {
                        (activity as? MainActivity)?.showAppContext(anchorView, app)
                    }
                }
            ))
            addClassPresenter(HdmiCardItem::class.java, HdmiCardPresenter(
                activeReorderKey = { activeReorderKey },
                onMove = { key, delta -> moveItemInPlace(key, delta) },
                onFinishReorder = { finishInPlaceReorder() },
                onHdmiClick = { hdmiItem ->
                    if (!isInPlaceReordering) {
                        (activity as? MainActivity)?.switchToHdmi(hdmiItem)
                    }
                },
                onHdmiLongClick = { anchorView, hdmiItem ->
                    if (!isInPlaceReordering) {
                        (activity as? MainActivity)?.showHdmiContext(anchorView, hdmiItem)
                    }
                }
            ))
            addClassPresenter(AddCardItem::class.java, AddAppCardPresenter {
                if (!isInPlaceReordering) {
                    (activity as? MainActivity)?.showAddAppsDialog()
                }
            })
        }
        mainSwimlaneAdapter = ArrayObjectAdapter(presenterSelector)
        rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.row_favorites)), mainSwimlaneAdapter))
        adapter = rowsAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (com.unix14.minimaltv.util.Motion.reduced) {
            verticalGridView?.itemAnimator = null
        }

        verticalGridView?.setOnKeyInterceptListener { event ->
            if (isInPlaceReordering) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MENU -> {
                            finishInPlaceReorder()
                            return@setOnKeyInterceptListener true
                        }
                        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                            return@setOnKeyInterceptListener true
                        }
                    }
                }
            }
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (selectedPosition == 0 && !isInPlaceReordering) {
                    val topBtn = requireActivity().findViewById<View>(R.id.btn_launcher_settings)
                    topBtn?.requestFocus()
                    return@setOnKeyInterceptListener true
                }
            }
            false
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favoriteApps.collectLatest { favorites ->
                        currentFavorites = favorites
                        updateMainSwimlane()
                    }
                }
                launch {
                    viewModel.homeOrder.collectLatest { order ->
                        currentOrder = order
                        updateMainSwimlane()
                    }
                }
                launch {
                    viewModel.previewChannels.collectLatest { channels ->
                        currentChannels = channels
                        updatePreviewRows()
                    }
                }
            }
        }
    }

    private fun updateMainSwimlane() {
        if (isInPlaceReordering) return
        val sorted = HomeOrder.sort(currentFavorites, HdmiCardItem.defaults(), currentOrder)
        if (itemsEqual(currentHomeItems, sorted) && mainSwimlaneAdapter.size() == sorted.size + 1) {
            return
        }
        val rowViewHolder = findRowViewHolderByPosition(0) as? androidx.leanback.widget.ListRowPresenter.ViewHolder
        val selectedPos = rowViewHolder?.gridView?.selectedPosition ?: 0
        currentHomeItems = sorted.toMutableList()
        mainSwimlaneAdapter.clear()
        for (item in currentHomeItems) {
            mainSwimlaneAdapter.add(item)
        }
        mainSwimlaneAdapter.add(AddCardItem())
        if (selectedPos in 0 until mainSwimlaneAdapter.size()) {
            view?.post {
                val holder = findRowViewHolderByPosition(0) as? androidx.leanback.widget.ListRowPresenter.ViewHolder
                holder?.gridView?.setSelectedPosition(selectedPos)
            }
        }
    }

    private fun itemsEqual(list1: List<Any>, list2: List<Any>): Boolean {
        if (list1.size != list2.size) return false
        for (i in list1.indices) {
            val k1 = HomeOrder.keyOf(list1[i])
            val k2 = HomeOrder.keyOf(list2[i])
            if (k1 != k2) return false
        }
        return true
    }

    private var displayedChannelSignatures: List<String> = emptyList()

    private fun updatePreviewRows() {
        val enabledChannels = currentChannels.filter { it.isEnabled && it.programs.isNotEmpty() }
        val newSignatures = enabledChannels.map { "${it.id}_${it.programs.size}_${it.programs.firstOrNull()?.id}" }
        if (newSignatures == displayedChannelSignatures) {
            return
        }
        displayedChannelSignatures = newSignatures
        while (rowsAdapter.size() > 1) {
            rowsAdapter.removeItems(1, rowsAdapter.size() - 1)
        }
        val programPresenter = ProgramCardPresenter()
        for ((index, channel) in enabledChannels.withIndex()) {
            val programAdapter = ArrayObjectAdapter(programPresenter)
            for (program in channel.programs) {
                programAdapter.add(program)
            }
            val headerTitle = if (channel.title.startsWith(channel.appName)) channel.title else "${channel.appName} - ${channel.title}"
            rowsAdapter.add(ListRow(HeaderItem((index + 1).toLong(), headerTitle), programAdapter))
        }
    }
}
