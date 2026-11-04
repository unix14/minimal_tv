package com.unix14.minimaltv.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unix14.minimaltv.MinimalTVApp
import com.unix14.minimaltv.R
import com.unix14.minimaltv.databinding.ActivityMainBinding
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.HdmiCardItem
import com.unix14.minimaltv.ui.dialog.AddAppsDialogFragment
import com.unix14.minimaltv.ui.dialog.AppContextPopupWindow
import com.unix14.minimaltv.ui.dialog.HdmiContextPopupWindow
import com.unix14.minimaltv.ui.dialog.OnboardingDialogFragment
import com.unix14.minimaltv.ui.settings.LauncherSettingsFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            MinimalTVApp.instance.appRepository,
            MinimalTVApp.instance.channelRepository,
            MinimalTVApp.instance.settingsRepository
        )
    }

    private var lastDataRefreshTime: Long = 0L

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lastDataRefreshTime = android.os.SystemClock.elapsedRealtime()
            viewModel.loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainRowsFragment())
                .commit()
        }

        setupTopBar()
        observeViewModel()
        registerPackageReceiver()
        checkPermissions()
        runEntranceAnimation()
    }

    private fun runEntranceAnimation() {
        binding.topBar.alpha = 0f
        binding.topBar.translationY = -resources.displayMetrics.density * 20
        binding.topBar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.mainBrowseFragment.alpha = 0f
        binding.mainBrowseFragment.translationY = resources.displayMetrics.density * 28
        binding.mainBrowseFragment.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setStartDelay(80)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun checkPermissions() {
        val perm = "android.permission.READ_TV_LISTINGS"
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 101)
        }
    }

    private fun setupTopBar() {
        binding.btnLauncherSettings.setOnClickListener { showSettings() }
        binding.settingsContainer.setOnClickListener { if (isInputsOpen) closeInputsDrawer() else if (isSettingsOpen) closeSettings() }

        binding.btnLauncherSettings.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
                fragment?.verticalGridView?.requestFocus()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentTime.collectLatest { time ->
                        binding.tvClock.text = time
                    }
                }
                launch {
                    viewModel.currentDate.collectLatest { date ->
                        binding.tvDate.text = date
                    }
                }
                launch {
                    viewModel.onboardingCompleted.collectLatest { completed ->
                        if (!completed) {
                            if (supportFragmentManager.findFragmentByTag("OnboardingDialog") == null) {
                                OnboardingDialogFragment.newInstance(viewModel)
                                    .show(supportFragmentManager, "OnboardingDialog")
                            }
                        }
                    }
                }
            }
        }
    }

    private val settingsFragment: LauncherSettingsFragment?
        get() = supportFragmentManager.findFragmentByTag(LauncherSettingsFragment.TAG) as? LauncherSettingsFragment

    val isSettingsOpen: Boolean get() = settingsFragment != null

    private val inputsFragment: com.unix14.minimaltv.ui.inputs.LauncherInputsFragment?
        get() = supportFragmentManager.findFragmentByTag(com.unix14.minimaltv.ui.inputs.LauncherInputsFragment.TAG) as? com.unix14.minimaltv.ui.inputs.LauncherInputsFragment

    val isInputsOpen: Boolean get() = inputsFragment != null

    /** Opens the native Android TV style settings drawer (full-height pane docked to the screen edge). */
    fun showSettings() {
        if (isSettingsOpen) return
        val currentInputs = inputsFragment
        val hadDrawerOpen = currentInputs != null
        if (currentInputs != null) {
            supportFragmentManager.beginTransaction().remove(currentInputs).commitNowAllowingStateLoss()
        }
        binding.topBar.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        binding.mainBrowseFragment.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        binding.settingsContainer.visibility = View.VISIBLE
        if (!hadDrawerOpen) {
            binding.settingsContainer.alpha = 0f
            binding.settingsContainer.animate().alpha(1f).setDuration(240).start()
        } else {
            binding.settingsContainer.alpha = 1f
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, LauncherSettingsFragment(), LauncherSettingsFragment.TAG)
            .commit()
    }

    fun closeSettings(restoreFocusToSettingsBtn: Boolean = true) {
        val fragment = settingsFragment ?: return
        binding.settingsContainer.animate().alpha(0f).setDuration(180).start()
        fragment.animateClose {
            if (!isFinishing && !isDestroyed) {
                supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
                binding.settingsContainer.visibility = View.GONE
                binding.settingsContainer.alpha = 1f
                binding.topBar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                binding.mainBrowseFragment.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                if (restoreFocusToSettingsBtn) {
                    binding.btnLauncherSettings.requestFocus()
                }
            }
        }
    }

    /** Opens the native Android TV style HDMI inputs drawer. */
    fun showInputsDrawer() {
        if (isInputsOpen) return
        val currentSettings = settingsFragment
        val hadDrawerOpen = currentSettings != null
        if (currentSettings != null) {
            supportFragmentManager.beginTransaction().remove(currentSettings).commitNowAllowingStateLoss()
        }
        binding.topBar.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        binding.mainBrowseFragment.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        binding.settingsContainer.visibility = View.VISIBLE
        if (!hadDrawerOpen) {
            binding.settingsContainer.alpha = 0f
            binding.settingsContainer.animate().alpha(1f).setDuration(240).start()
        } else {
            binding.settingsContainer.alpha = 1f
        }
        val fragment = com.unix14.minimaltv.ui.inputs.LauncherInputsFragment().apply {
            onInputSelected = { closeInputsDrawer() }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_container, fragment, com.unix14.minimaltv.ui.inputs.LauncherInputsFragment.TAG)
            .commit()
    }

    fun closeInputsDrawer() {
        val fragment = inputsFragment ?: return
        binding.settingsContainer.animate().alpha(0f).setDuration(180).start()
        fragment.animateClose {
            if (!isFinishing && !isDestroyed) {
                supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
                binding.settingsContainer.visibility = View.GONE
                binding.settingsContainer.alpha = 1f
                binding.topBar.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
                binding.mainBrowseFragment.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) {
                handleHomeButtonPressed()
            }
            return true
        }

        if (com.unix14.minimaltv.util.LauncherHelper.isTvInputKey(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (isInputsOpen) closeInputsDrawer() else showInputsDrawer()
            }
            return true
        }

        if (isInputsOpen) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) {
                    closeInputsDrawer()
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        val settings = settingsFragment
        if (settings != null) {
            // While the settings drawer is open, Back walks up the settings pages and finally closes the drawer
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP && !settings.handleBack()) {
                    closeSettings()
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
            if (fragment != null && fragment.selectedPosition == 0) {
                val focused = currentFocus
                if (focused != binding.btnLauncherSettings) {
                    binding.btnLauncherSettings.requestFocus()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleHomeButtonPressed() {
        if (isInputsOpen) {
            closeInputsDrawer()
        }
        if (isSettingsOpen) {
            closeSettings(restoreFocusToSettingsBtn = false)
        }
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
        if (fragment?.isInPlaceReordering == true) {
            fragment.finishInPlaceReorder()
        }
        if (fragment != null) {
            fragment.setSelectedPosition(0)
            fragment.verticalGridView?.requestFocus()
        }
    }

    fun showAppContext(anchorView: View, app: AppModel) {
        AppContextPopupWindow(this, app, viewModel).show(anchorView)
    }

    fun showAddAppsDialog() {
        AddAppsDialogFragment.newInstance(viewModel)
            .show(supportFragmentManager, "AddAppsDialog")
    }

    fun showHdmiContext(anchorView: View, hdmi: HdmiCardItem) {
        HdmiContextPopupWindow(
            context = this,
            hdmi = hdmi,
            onOpen = { switchToHdmi(hdmi) },
            onReorder = { startInPlaceReorder(hdmi.orderKey) }
        ).show(anchorView)
    }

    /** Starts moving a card (app package name or HDMI order key) in place on the first swimlane. */
    fun startInPlaceReorder(key: String) {
        closeSettings(restoreFocusToSettingsBtn = false)
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
        fragment?.startInPlaceReorder(key)
    }

    fun startFirstRowReorder() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
        val firstKey = fragment?.firstReorderableKey() ?: return
        startInPlaceReorder(firstKey)
    }

    fun switchToHdmi(hdmiItem: HdmiCardItem) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.media.tv.TvContract.buildChannelUriForPassthroughInput(hdmiItem.inputId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.mediatek.wwtv.tvcenter")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastDataRefreshTime == 0L || now - lastDataRefreshTime > 120_000L) {
            lastDataRefreshTime = now
            viewModel.loadData()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleHomeButtonPressed()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isInputsOpen) {
            closeInputsDrawer()
            return
        }
        if (isSettingsOpen) {
            if (settingsFragment?.handleBack() != true) closeSettings()
            return
        }
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
        if (fragment?.isInPlaceReordering == true) {
            fragment.finishInPlaceReorder()
            return
        }
        if (fragment != null && fragment.selectedPosition > 0) {
            fragment.setSelectedPosition(0)
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageChangeReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageChangeReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}
