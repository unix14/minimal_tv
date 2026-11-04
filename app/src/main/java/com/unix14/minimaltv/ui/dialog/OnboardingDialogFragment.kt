package com.unix14.minimaltv.ui.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.unix14.minimaltv.R
import com.unix14.minimaltv.ui.main.MainViewModel
import com.unix14.minimaltv.util.LauncherHelper

class OnboardingDialogFragment : DialogFragment() {

    private lateinit var viewModel: MainViewModel

    companion object {
        fun newInstance(viewModel: MainViewModel): OnboardingDialogFragment {
            val fragment = OnboardingDialogFragment()
            fragment.viewModel = viewModel
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (660 * resources.displayMetrics.density).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivLogo = view.findViewById<ImageView>(R.id.iv_onboarding_logo)
        val tvStatus = view.findViewById<TextView>(R.id.tv_onboarding_default_status)
        val btnSetDefault = view.findViewById<Button>(R.id.btn_onboarding_set_default)
        val btnAccessibility = view.findViewById<Button>(R.id.btn_onboarding_enable_accessibility)
        val btnStart = view.findViewById<Button>(R.id.btn_onboarding_start)

        // Sleek, zero-memory logo entrance animation
        ivLogo.scaleX = 0.7f
        ivLogo.scaleY = 0.7f
        ivLogo.alpha = 0f
        ivLogo.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(450)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()

        updateStatus(tvStatus, btnSetDefault)

        btnSetDefault.setOnClickListener {
            LauncherHelper.openSetDefaultLauncher(requireContext())
        }

        btnAccessibility.setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                // Ignore
            }
        }

        btnStart.setOnClickListener {
            viewModel.setOnboardingCompleted(true)
            dismiss()
        }

        btnStart.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            val tvStatus = it.findViewById<TextView>(R.id.tv_onboarding_default_status)
            val btnSetDefault = it.findViewById<Button>(R.id.btn_onboarding_set_default)
            updateStatus(tvStatus, btnSetDefault)
        }
    }

    private fun updateStatus(tvStatus: TextView, btnSetDefault: Button) {
        val isDefault = LauncherHelper.isDefaultLauncher(requireContext())
        if (isDefault) {
            tvStatus.setText(R.string.settings_default_launcher_active)
            btnSetDefault.visibility = View.GONE
        } else {
            tvStatus.setText(R.string.onboarding_step_launcher)
            btnSetDefault.visibility = View.VISIBLE
        }
    }
}
