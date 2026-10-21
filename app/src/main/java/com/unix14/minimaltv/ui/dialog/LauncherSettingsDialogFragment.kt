package com.unix14.minimaltv.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.unix14.minimaltv.R
import com.unix14.minimaltv.databinding.DialogLauncherSettingsBinding
import com.unix14.minimaltv.ui.main.MainViewModel

class LauncherSettingsDialogFragment : DialogFragment() {

    private var _binding: DialogLauncherSettingsBinding? = null
    private val binding get() = _binding!!
    private var viewModel: MainViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogLauncherSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val vm = viewModel ?: run { dismiss(); return }

        // Language buttons
        binding.btnLangEn.setOnClickListener {
            vm.setAppLocale("en")
            dismiss()
        }

        binding.btnLangHe.setOnClickListener {
            vm.setAppLocale("iw")
            dismiss()
        }

        // Accessibility service status
        val isA11yActive = vm.isAccessibilityEnabled()
        if (isA11yActive) {
            binding.tvAccessibilityStatus.text = getString(R.string.settings_accessibility_enabled_status)
            binding.tvAccessibilityStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.card_border_focused))
        } else {
            binding.tvAccessibilityStatus.text = getString(R.string.settings_accessibility_disabled_status)
            binding.tvAccessibilityStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_gold))
        }

        binding.btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        // Preview Channels list
        val channels = vm.previewChannels.value
        binding.llChannelsContainer.removeAllViews()
        if (channels.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = getString(R.string.settings_channels_empty)
                setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary))
                textSize = 13f
            }
            binding.llChannelsContainer.addView(emptyTv)
        } else {
            val inflater = LayoutInflater.from(requireContext())
            for (channel in channels) {
                val itemView = inflater.inflate(R.layout.item_channel_toggle, binding.llChannelsContainer, false)
                val titleTv = itemView.findViewById<TextView>(R.id.tv_channel_name)
                val checkBox = itemView.findViewById<CheckBox>(R.id.cb_channel_enabled)

                titleTv.text = "${channel.appName}: ${channel.title}"
                checkBox.isChecked = channel.isEnabled

                itemView.setOnClickListener {
                    val newState = !checkBox.isChecked
                    checkBox.isChecked = newState
                    vm.setChannelEnabled(channel.id, newState)
                }
                binding.llChannelsContainer.addView(itemView)
            }
        }

        binding.btnOpenAdbGuide.setOnClickListener {
            AdbGuideDialogFragment().show(parentFragmentManager, "AdbGuideDialog")
        }

        binding.btnCloseSettings.setOnClickListener {
            dismiss()
        }

        binding.btnLangEn.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(vm: MainViewModel): LauncherSettingsDialogFragment {
            return LauncherSettingsDialogFragment().apply {
                viewModel = vm
            }
        }
    }
}
