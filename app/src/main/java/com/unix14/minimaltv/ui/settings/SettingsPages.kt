package com.unix14.minimaltv.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.widget.BaseGridView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.unix14.minimaltv.MinimalTVApp
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.domain.model.ChannelModel
import com.unix14.minimaltv.domain.model.HdmiCardItem
import com.unix14.minimaltv.service.HomeInterceptorAccessibilityService
import com.unix14.minimaltv.ui.main.MainActivity
import com.unix14.minimaltv.ui.main.MainViewModel
import com.unix14.minimaltv.ui.main.MainViewModelFactory
import com.unix14.minimaltv.util.LauncherHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private const val ICON_COLOR = 0xFFEEEEEE.toInt()
private const val ICON_COLOR_DIM = 0x66EEEEEE
private const val ICON_COLOR_ACTIVE = 0xFF818CF8.toInt()

/** Common plumbing for every page of the settings drawer. */
abstract class BaseSettingsPage : LeanbackPreferenceFragmentCompat() {

    protected val vm: MainViewModel by activityViewModels {
        MainViewModelFactory(
            MinimalTVApp.instance.appRepository,
            MinimalTVApp.instance.channelRepository,
            MinimalTVApp.instance.settingsRepository
        )
    }

    protected val mainActivity: MainActivity get() = requireActivity() as MainActivity

    @get:StringRes
    protected abstract val pageTitleRes: Int

    protected abstract fun buildPreferences(screen: PreferenceScreen)

    /** Lets a page handle Back itself (e.g. dropping a picked-up row). Return true if consumed. */
    open fun consumeBack(): Boolean = false

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(preferenceManager.context)
        screen.title = getString(pageTitleRes)
        preferenceScreen = screen
        buildPreferences(screen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.post { listView.requestFocus() }
    }

    protected fun newPreference(
        title: CharSequence,
        summary: CharSequence? = null,
        @DrawableRes iconRes: Int = 0,
        selectable: Boolean = true,
        onClick: (() -> Unit)? = null
    ): Preference {
        return Preference(preferenceManager.context).apply {
            this.title = title
            this.summary = summary
            isPersistent = false
            isSelectable = selectable
            if (iconRes != 0) icon = tintedIcon(iconRes, ICON_COLOR)
            if (onClick != null) {
                setOnPreferenceClickListener {
                    onClick()
                    true
                }
            }
        }
    }

    protected fun navigationPreference(
        @StringRes title: Int,
        @StringRes summary: Int,
        @DrawableRes iconRes: Int,
        page: Class<out BaseSettingsPage>
    ): Preference {
        return newPreference(getString(title), getString(summary), iconRes).apply {
            fragment = page.name
        }
    }

    protected fun tintedIcon(@DrawableRes res: Int, color: Int): Drawable? {
        return ContextCompat.getDrawable(requireContext(), res)?.mutate()?.also { it.setTint(color) }
    }

    protected fun infoPreference(text: CharSequence): Preference {
        return newPreference(title = text, selectable = false).apply { isIconSpaceReserved = false }
    }
}

class RootSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_title

    override fun buildPreferences(screen: PreferenceScreen) {
        screen.addPreference(
            navigationPreference(R.string.settings_cat_channels, R.string.settings_cat_channels_desc, R.drawable.ic_tv, ChannelsSettingsPage::class.java)
        )
        screen.addPreference(
            navigationPreference(R.string.settings_cat_home_apps, R.string.settings_cat_home_apps_desc, R.drawable.ic_reorder, HomeAppsSettingsPage::class.java)
        )
        screen.addPreference(
            navigationPreference(R.string.settings_cat_hdmi, R.string.settings_cat_hdmi_desc, R.drawable.ic_gamepad, HdmiSettingsPage::class.java)
        )
        screen.addPreference(
            navigationPreference(R.string.settings_cat_system, R.string.settings_cat_system_desc, R.drawable.ic_settings, SystemSettingsPage::class.java)
        )
        screen.addPreference(
            navigationPreference(R.string.settings_cat_about, R.string.settings_cat_about_desc, R.drawable.ic_info, AboutSettingsPage::class.java)
        )
    }
}

/** Preview channels grouped by app, each with its own on/off switch. Nothing is shown on home until switched on. */
class ChannelsSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_cat_channels

    private var builtSignature: List<Pair<Long, String>>? = null
    private var orderPreference: Preference? = null

    override fun buildPreferences(screen: PreferenceScreen) {
        rebuild(screen, vm.previewChannels.value)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.previewChannels.collectLatest { channels ->
                    if (signatureOf(channels) != builtSignature) {
                        rebuild(preferenceScreen, channels)
                    } else {
                        updateOrderPreference(channels)
                    }
                }
            }
        }
    }

    private fun signatureOf(channels: List<ChannelModel>) = channels.map { it.id to it.title }.sortedBy { it.first }

    private fun rebuild(screen: PreferenceScreen, channels: List<ChannelModel>) {
        screen.removeAll()
        builtSignature = signatureOf(channels)
        orderPreference = null

        if (channels.isEmpty()) {
            screen.addPreference(infoPreference(getString(R.string.settings_channels_empty)))
            return
        }

        orderPreference = navigationPreference(
            R.string.settings_reorder_rows,
            R.string.settings_reorder_rows_hint,
            R.drawable.ic_reorder,
            ChannelOrderPage::class.java
        ).also { screen.addPreference(it) }
        updateOrderPreference(channels)

        val byApp = channels
            .sortedWith(compareBy({ it.appName.lowercase() }, { it.title.lowercase() }))
            .groupBy { it.appName }
        for ((appName, appChannels) in byApp) {
            val category = PreferenceCategory(preferenceManager.context).apply { title = appName }
            screen.addPreference(category)
            for (channel in appChannels) {
                category.addPreference(
                    SwitchPreferenceCompat(preferenceManager.context).apply {
                        key = "channel_${channel.id}"
                        title = channel.title
                        summary = getString(R.string.settings_channel_items, channel.programs.size)
                        isPersistent = false
                        isChecked = channel.isEnabled
                        isIconSpaceReserved = false
                        setOnPreferenceChangeListener { _, newValue ->
                            vm.setChannelEnabled(channel.id, newValue as Boolean)
                            true
                        }
                    }
                )
            }
        }
    }

    private fun updateOrderPreference(channels: List<ChannelModel>) {
        val pref = orderPreference ?: return
        val enabledCount = channels.count { it.isEnabled }
        pref.isEnabled = enabledCount >= 2
        pref.summary = if (enabledCount >= 2) {
            getString(R.string.settings_reorder_rows_summary, enabledCount)
        } else {
            getString(R.string.settings_reorder_rows_need_two)
        }
    }
}

/**
 * Reordering of the rows enabled on the home screen. Press OK on a row to pick it up, Up / Down moves it,
 * OK (or Back) drops it. Everything is reachable with the D-pad; there are no tiny arrow buttons.
 */
class ChannelOrderPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_reorder_rows

    private var grabbedId: Long? = null
    private val channelPreferences = LinkedHashMap<Long, Preference>()
    private var latestEnabled: List<ChannelModel> = emptyList()

    override fun consumeBack(): Boolean {
        if (grabbedId == null) return false
        grabbedId = null
        refreshLabels()
        return true
    }

    override fun buildPreferences(screen: PreferenceScreen) {
        rebuild(screen, enabledChannels(vm.previewChannels.value))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (listView as? BaseGridView)?.setOnKeyInterceptListener { event ->
            val id = grabbedId ?: return@setOnKeyInterceptListener false
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        vm.moveChannel(id, if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                    }
                    true
                }
                else -> false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.previewChannels.collectLatest { channels -> applyChannels(enabledChannels(channels)) }
            }
        }
    }

    private fun enabledChannels(channels: List<ChannelModel>) = channels.filter { it.isEnabled }

    private fun applyChannels(enabled: List<ChannelModel>) {
        if (enabled.map { it.id }.toSet() != channelPreferences.keys) {
            grabbedId = grabbedId?.takeIf { id -> enabled.any { it.id == id } }
            rebuild(preferenceScreen, enabled)
            return
        }
        latestEnabled = enabled
        enabled.forEachIndexed { index, channel -> channelPreferences[channel.id]?.order = index + 1 }
        refreshLabels()
        val grabbed = grabbedId ?: return
        val position = enabled.indexOfFirst { it.id == grabbed }
        // +1 for the hint row above the channels
        listView.post { (listView as? BaseGridView)?.setSelectedPosition(position + 1) }
    }

    private fun rebuild(screen: PreferenceScreen, enabled: List<ChannelModel>) {
        screen.removeAll()
        channelPreferences.clear()
        latestEnabled = enabled

        if (enabled.isEmpty()) {
            screen.addPreference(infoPreference(getString(R.string.settings_reorder_rows_none)))
            return
        }

        screen.addPreference(infoPreference(getString(R.string.settings_reorder_rows_hint)).apply { order = 0 })
        enabled.forEachIndexed { index, channel ->
            val pref = newPreference(channel.title) {
                grabbedId = if (grabbedId == channel.id) null else channel.id
                refreshLabels()
            }.apply {
                key = "order_${channel.id}"
                order = index + 1
            }
            channelPreferences[channel.id] = pref
            screen.addPreference(pref)
        }
        refreshLabels()
    }

    private fun refreshLabels() {
        for (channel in latestEnabled) {
            val pref = channelPreferences[channel.id] ?: continue
            val isGrabbed = grabbedId == channel.id
            pref.title = channel.title
            pref.summary = if (isGrabbed) getString(R.string.settings_reorder_rows_active) else channel.appName
            pref.icon = tintedIcon(R.drawable.ic_reorder, if (isGrabbed) ICON_COLOR_ACTIVE else ICON_COLOR_DIM)
        }
    }
}

/** Pick which apps appear on the first swimlane, and jump into in-place reordering of that swimlane. */
class HomeAppsSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_cat_home_apps

    private var builtPackages: Set<String>? = null

    override fun buildPreferences(screen: PreferenceScreen) {
        rebuild(screen, vm.installedApps.value)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.installedApps.collectLatest { apps ->
                    if (apps.map { it.packageName }.toSet() != builtPackages) {
                        rebuild(preferenceScreen, apps)
                    }
                }
            }
        }
    }

    private fun rebuild(screen: PreferenceScreen, apps: List<AppModel>) {
        screen.removeAll()
        builtPackages = apps.map { it.packageName }.toSet()

        screen.addPreference(
            newPreference(
                title = getString(R.string.settings_reorder_first_row),
                summary = getString(R.string.settings_reorder_first_row_desc),
                iconRes = R.drawable.ic_reorder
            ) { mainActivity.startFirstRowReorder() }
        )
        screen.addPreference(
            newPreference(
                title = getString(R.string.settings_manage_apps_btn),
                iconRes = R.drawable.ic_add
            ) { mainActivity.showAddAppsDialog() }
        )

        val hdmiCategory = PreferenceCategory(preferenceManager.context).apply {
            title = getString(R.string.settings_cat_hdmi)
        }
        screen.addPreference(hdmiCategory)
        for (item in HdmiCardItem.defaults()) {
            hdmiCategory.addPreference(
                newPreference(
                    title = item.title,
                    summary = getString(R.string.settings_hdmi_move_summary, item.subtitle),
                    iconRes = item.iconRes
                ) { mainActivity.startInPlaceReorder(item.orderKey) }
            )
        }

        val category = PreferenceCategory(preferenceManager.context).apply {
            title = getString(R.string.settings_home_apps_category)
        }
        screen.addPreference(category)
        for (app in apps.sortedBy { it.label.lowercase() }) {
            category.addPreference(
                SwitchPreferenceCompat(preferenceManager.context).apply {
                    key = "app_${app.packageName}"
                    title = app.label
                    isPersistent = false
                    isChecked = app.isFavorite
                    app.icon?.constantState?.newDrawable()?.mutate()?.let { icon = it }
                    setOnPreferenceChangeListener { _, newValue ->
                        if (newValue as Boolean) vm.pinFavorite(app.packageName) else vm.unpinFavorite(app.packageName)
                        true
                    }
                }
            )
        }
    }
}

/** PS5 / Xbox shortcuts: each entry jumps into in-place reordering with that card picked up. */
class HdmiSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_cat_hdmi

    override fun buildPreferences(screen: PreferenceScreen) {
        for (item in HdmiCardItem.defaults()) {
            screen.addPreference(
                newPreference(
                    title = item.title,
                    summary = getString(R.string.settings_hdmi_move_summary, item.subtitle),
                    iconRes = item.iconRes
                ) { mainActivity.startInPlaceReorder(item.orderKey) }
            )
        }
        screen.addPreference(
            newPreference(
                title = getString(R.string.settings_open_tv_center),
                iconRes = R.drawable.ic_tv
            ) {
                mainActivity.showInputsDrawer()
            }
        )
    }
}

class SystemSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_cat_system

    private var defaultLauncherPreference: Preference? = null
    private var accessibilityPreference: Preference? = null

    override fun buildPreferences(screen: PreferenceScreen) {
        screen.addPreference(
            newPreference(
                title = getString(R.string.settings_system_settings),
                summary = getString(R.string.settings_system_settings_desc),
                iconRes = R.drawable.ic_settings
            ) {
                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    // Settings app not available
                }
            }
        )
        defaultLauncherPreference = newPreference(
            title = getString(R.string.settings_default_launcher),
            iconRes = R.drawable.ic_tv
        ) {
            if (!LauncherHelper.isDefaultLauncher(requireContext())) {
                LauncherHelper.openSetDefaultLauncher(requireContext())
            }
        }.also { screen.addPreference(it) }
        accessibilityPreference = newPreference(
            title = getString(R.string.settings_accessibility),
            iconRes = R.drawable.ic_tune
        ) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.also { screen.addPreference(it) }
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val context = context ?: return
        defaultLauncherPreference?.summary = getString(
            if (LauncherHelper.isDefaultLauncher(context)) R.string.settings_default_launcher_active
            else R.string.settings_default_launcher_inactive
        )
        accessibilityPreference?.summary = getString(
            if (isAccessibilityServiceEnabled(context)) R.string.settings_accessibility_enabled_status
            else R.string.settings_accessibility_disabled_status
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${HomeInterceptorAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":").any { it.equals(expectedServiceName, ignoreCase = true) }
    }
}

class AboutSettingsPage : BaseSettingsPage() {
    override val pageTitleRes = R.string.settings_cat_about

    override fun buildPreferences(screen: PreferenceScreen) {
        screen.addPreference(
            newPreference(
                title = getString(R.string.about_title),
                summary = getString(R.string.about_desc),
                selectable = false
            ).apply { isIconSpaceReserved = false }
        )
    }
}
