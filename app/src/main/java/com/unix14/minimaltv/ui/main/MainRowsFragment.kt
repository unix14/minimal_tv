package com.unix14.minimaltv.ui.main

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unix14.minimaltv.MinimalTVApp
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.ui.presenter.AppCardPresenter
import com.unix14.minimaltv.ui.presenter.CustomListRowPresenter
import com.unix14.minimaltv.ui.presenter.ProgramCardPresenter
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

    private var currentFavorites: List<AppModel> = emptyList()
    private var currentInstalled: List<AppModel> = emptyList()
    private var currentChannels: List<ChannelModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = rowsAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Strict D-Pad focus retention: intercept UP key from row 0 to top bar
        verticalGridView?.setOnKeyInterceptListener { event ->
            if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                if (selectedPosition == 0) {
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
                        rebuildRows()
                    }
                }
                launch {
                    viewModel.installedApps.collectLatest { apps ->
                        currentInstalled = apps
                        rebuildRows()
                    }
                }
                launch {
                    viewModel.previewChannels.collectLatest { channels ->
                        currentChannels = channels
                        rebuildRows()
                    }
                }
            }
        }
    }

    private fun rebuildRows() {
        rowsAdapter.clear()

        // 1. Favorites Row
        if (currentFavorites.isNotEmpty()) {
            val favPresenter = AppCardPresenter { app ->
                (activity as? MainActivity)?.showAppContext(app)
            }
            val favAdapter = ArrayObjectAdapter(favPresenter)
            for (app in currentFavorites) {
                favAdapter.add(app)
            }
            rowsAdapter.add(ListRow(HeaderItem(0, getString(R.string.row_favorites)), favAdapter))
        }

        // 2. All Installed Apps Row
        val appsPresenter = AppCardPresenter { app ->
            (activity as? MainActivity)?.showAppContext(app)
        }
        val appsAdapter = ArrayObjectAdapter(appsPresenter)
        for (app in currentInstalled) {
            appsAdapter.add(app)
        }
        rowsAdapter.add(ListRow(HeaderItem(1, getString(R.string.row_apps)), appsAdapter))

        // 3. Android TV Preview Channels
        val enabledChannels = currentChannels.filter { it.isEnabled }
        val programPresenter = ProgramCardPresenter()
        for ((index, channel) in enabledChannels.withIndex()) {
            val programAdapter = ArrayObjectAdapter(programPresenter)
            for (program in channel.programs) {
                programAdapter.add(program)
            }
            val headerTitle = if (channel.title.startsWith(channel.appName)) channel.title else "${channel.appName} - ${channel.title}"
            rowsAdapter.add(ListRow(HeaderItem((index + 2).toLong(), headerTitle), programAdapter))
        }
    }
}
