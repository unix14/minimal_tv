package com.unix14.minimaltv.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.unix14.minimaltv.R
import com.unix14.minimaltv.databinding.DialogAppContextBinding
import com.unix14.minimaltv.domain.model.AppModel
import com.unix14.minimaltv.ui.main.MainViewModel

class AppContextModalDialogFragment : DialogFragment() {

    private var _binding: DialogAppContextBinding? = null
    private val binding get() = _binding!!
    private var targetApp: AppModel? = null
    private var viewModel: MainViewModel? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAppContextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = targetApp ?: run { dismiss(); return }

        binding.tvModalAppTitle.text = app.label
        if (app.bannerOrIcon != null) {
            binding.ivModalAppIcon.setImageDrawable(app.bannerOrIcon)
        } else {
            binding.ivModalAppIcon.setImageResource(R.drawable.ic_tv)
        }

        binding.btnToggleFavorite.text = if (app.isFavorite) {
            getString(R.string.action_unpin_favorite)
        } else {
            getString(R.string.action_pin_favorite)
        }

        binding.btnMoveLeft.setOnClickListener {
            viewModel?.moveApp(app.packageName, -1)
            dismiss()
        }

        binding.btnMoveRight.setOnClickListener {
            viewModel?.moveApp(app.packageName, 1)
            dismiss()
        }

        binding.btnToggleFavorite.setOnClickListener {
            if (app.isFavorite) {
                viewModel?.unpinFavorite(app.packageName)
            } else {
                viewModel?.pinFavorite(app.packageName)
            }
            dismiss()
        }

        binding.btnAppInfo.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            dismiss()
        }

        binding.btnUninstall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            dismiss()
        }

        binding.btnHideApp.setOnClickListener {
            viewModel?.hideApp(app.packageName)
            dismiss()
        }

        binding.btnCloseContext.setOnClickListener {
            dismiss()
        }

        binding.btnMoveLeft.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(app: AppModel, vm: MainViewModel): AppContextModalDialogFragment {
            return AppContextModalDialogFragment().apply {
                targetApp = app
                viewModel = vm
            }
        }
    }
}
