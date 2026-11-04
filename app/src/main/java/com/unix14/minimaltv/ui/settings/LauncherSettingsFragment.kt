package com.unix14.minimaltv.ui.settings

import android.os.Bundle
import android.view.View
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen

/**
 * MinimalTV settings drawer, built on the stock Android TV settings components
 * (LeanbackSettingsFragmentCompat + LeanbackPreferenceFragmentCompat pages), so it looks and
 * behaves like the system Settings panel: a full-height pane docked to the screen edge.
 */
class LauncherSettingsFragment : LeanbackSettingsFragmentCompat() {

    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(RootSettingsPage())
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val className = pref.fragment ?: return false
        val page = childFragmentManager.fragmentFactory.instantiate(requireActivity().classLoader, className)
        page.arguments = pref.extras
        startPreferenceFragment(page)
        return true
    }

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = view.findViewById<View>(androidx.leanback.preference.R.id.settings_preference_fragment_container)
        if (container != null) {
            val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val paneWidth = resources.getDimension(com.unix14.minimaltv.R.dimen.lb_settings_pane_width)
            container.translationX = if (isRtl) -paneWidth else paneWidth
            container.animate().translationX(0f).setDuration(260).setInterpolator(android.view.animation.DecelerateInterpolator(1.8f)).start()
        }
    }

    fun animateClose(onEnd: () -> Unit) {
        val v = view ?: run { onEnd(); return }
        val container = v.findViewById<View>(androidx.leanback.preference.R.id.settings_preference_fragment_container) ?: run { onEnd(); return }
        val isRtl = v.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val paneWidth = resources.getDimension(com.unix14.minimaltv.R.dimen.lb_settings_pane_width)
        val targetX = if (isRtl) -paneWidth else paneWidth
        container.animate().translationX(targetX).setDuration(200).setInterpolator(android.view.animation.AccelerateInterpolator(1.8f)).withEndAction(onEnd).start()
    }

    /** Returns true when the Back press was consumed here (inner page action or navigating up one page). */
    fun handleBack(): Boolean {
        val currentPage = childFragmentManager.fragments.filterIsInstance<BaseSettingsPage>().lastOrNull()
        if (currentPage?.consumeBack() == true) return true
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }
        return false
    }

    companion object {
        const val TAG = "LauncherSettings"
    }
}
