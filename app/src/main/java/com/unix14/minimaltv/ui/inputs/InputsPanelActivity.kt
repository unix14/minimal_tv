package com.unix14.minimaltv.ui.inputs

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.unix14.minimaltv.R

class InputsPanelActivity : AppCompatActivity() {

    private var inputsFragment: LauncherInputsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inputs_panel)

        findViewById<View>(R.id.inputs_scrim)?.setOnClickListener { finishWithAnimation() }

        val fragment = LauncherInputsFragment().apply {
            onInputSelected = { finish() }
        }
        inputsFragment = fragment

        supportFragmentManager.beginTransaction()
            .replace(R.id.inputs_container, fragment, LauncherInputsFragment.TAG)
            .commit()
    }

    private fun finishWithAnimation() {
        val fragment = inputsFragment
        if (fragment != null) {
            fragment.animateClose { finish() }
        } else {
            finish()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                finishWithAnimation()
            }
            return true
        }
        if (com.unix14.minimaltv.util.LauncherHelper.isTvInputKey(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                finishWithAnimation()
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            finish()
            return false
        }
        return super.dispatchKeyEvent(event)
    }
}
