package com.unix14.minimaltv.ui.inputs

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.unix14.minimaltv.R
import com.unix14.minimaltv.domain.model.TvInputHelper
import com.unix14.minimaltv.domain.model.TvInputItem

class InputsPage : LeanbackPreferenceFragmentCompat() {

    var onInputSelected: (() -> Unit)? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(preferenceManager.context)
        screen.title = getString(R.string.inputs_title)
        preferenceScreen = screen

        val inputs = TvInputHelper.getAvailableInputs(requireContext())
        for (input in inputs) {
            screen.addPreference(createInputPreference(input))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.post { listView.requestFocus() }
    }

    private fun createInputPreference(input: TvInputItem): Preference {
        return Preference(preferenceManager.context).apply {
            title = input.title
            summary = input.subtitle
            isPersistent = false
            icon = tintedIcon(input.iconRes, 0xFFEEEEEE.toInt())
            setOnPreferenceClickListener {
                TvInputHelper.switchToInput(requireContext(), input)
                onInputSelected?.invoke()
                true
            }
        }
    }

    private fun tintedIcon(@DrawableRes res: Int, color: Int): Drawable? {
        return ContextCompat.getDrawable(requireContext(), res)?.mutate()?.also { it.setTint(color) }
    }
}
