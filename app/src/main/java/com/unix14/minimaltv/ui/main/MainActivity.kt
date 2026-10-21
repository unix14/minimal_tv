package com.unix14.minimaltv.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.unix14.minimaltv.MinimalTVApp
import com.unix14.minimaltv.R
import com.unix14.minimaltv.databinding.ActivityMainBinding
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.ui.dialog.AppContextModalDialogFragment
import com.unix14.minimaltv.ui.dialog.LauncherSettingsDialogFragment
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

    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
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
    }

    private fun setupTopBar() {
        binding.btnSystemSettings.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                // Ignore if settings cannot be opened
            }
        }

        binding.btnLauncherSettings.setOnClickListener {
            LauncherSettingsDialogFragment.newInstance(viewModel)
                .show(supportFragmentManager, "LauncherSettingsDialog")
        }

        val focusDownListener = View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
                fragment?.verticalGridView?.requestFocus()
                return@OnKeyListener true
            }
            false
        }
        binding.btnSystemSettings.setOnKeyListener(focusDownListener)
        binding.btnLauncherSettings.setOnKeyListener(focusDownListener)
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
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
            if (fragment != null && fragment.selectedPosition == 0) {
                val focused = currentFocus
                if (focused != binding.btnLauncherSettings && focused != binding.btnSystemSettings) {
                    binding.btnLauncherSettings.requestFocus()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    fun showAppContext(app: AppModel) {
        AppContextModalDialogFragment.newInstance(app, viewModel)
            .show(supportFragmentManager, "AppContextDialog")
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = supportFragmentManager.findFragmentById(R.id.main_browse_fragment) as? MainRowsFragment
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
