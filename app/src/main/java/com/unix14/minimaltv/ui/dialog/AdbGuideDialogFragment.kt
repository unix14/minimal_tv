package com.unix14.minimaltv.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.unix14.minimaltv.R
import com.unix14.minimaltv.databinding.DialogAdbGuideBinding

class AdbGuideDialogFragment : DialogFragment() {

    private var _binding: DialogAdbGuideBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAdbGuideBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvAdbGuideContent.text = getString(R.string.adb_guide_content, "192.168.68.121")
        binding.btnCloseAdbGuide.setOnClickListener { dismiss() }
        binding.btnCloseAdbGuide.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
