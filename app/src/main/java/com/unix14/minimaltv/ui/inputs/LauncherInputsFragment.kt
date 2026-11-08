package com.unix14.minimaltv.ui.inputs

import android.os.Bundle
import android.view.View
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.unix14.minimaltv.R

class LauncherInputsFragment : LeanbackSettingsFragmentCompat() {

    var onInputSelected: (() -> Unit)? = null

    override fun onPreferenceStartInitialScreen() {
        val page = InputsPage().apply {
            this.onInputSelected = this@LauncherInputsFragment.onInputSelected
        }
        startPreferenceFragment(page)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean = false

    override fun onPreferenceStartScreen(caller: PreferenceFragmentCompat, pref: PreferenceScreen): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val container = view.findViewById<View>(androidx.leanback.preference.R.id.settings_preference_fragment_container)
        if (container != null) {
            val isRtl = view.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val paneWidth = resources.getDimension(R.dimen.lb_settings_pane_width)
            container.translationX = if (isRtl) -paneWidth else paneWidth
            container.animate().translationX(0f).setDuration(com.unix14.minimaltv.util.Motion.duration(260)).setInterpolator(android.view.animation.DecelerateInterpolator(1.8f)).start()
        }
    }

    fun animateClose(onEnd: () -> Unit) {
        val v = view ?: run { onEnd(); return }
        val container = v.findViewById<View>(androidx.leanback.preference.R.id.settings_preference_fragment_container) ?: run { onEnd(); return }
        val isRtl = v.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val paneWidth = resources.getDimension(R.dimen.lb_settings_pane_width)
        val targetX = if (isRtl) -paneWidth else paneWidth
        container.animate().translationX(targetX).setDuration(com.unix14.minimaltv.util.Motion.duration(200)).setInterpolator(android.view.animation.AccelerateInterpolator(1.8f)).withEndAction(onEnd).start()
    }

    companion object {
        const val TAG = "LauncherInputs"
    }
}
