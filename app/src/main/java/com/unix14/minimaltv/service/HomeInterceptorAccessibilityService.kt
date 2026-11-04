package com.unix14.minimaltv.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.unix14.minimaltv.ui.main.MainActivity

class HomeInterceptorAccessibilityService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        android.util.Log.d("HomeInterceptor", "onKeyEvent keyCode=${event.keyCode} action=${event.action}")
        if (com.unix14.minimaltv.util.LauncherHelper.isTvInputKey(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                com.unix14.minimaltv.util.LauncherHelper.openInputSource(this)
            }
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) {
                launchLauncher()
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: ""
        // Intercept Xiaomi PatchWall and Google TV launcher takeovers, but preserve the native Inputs side drawer panel
        if (pkg == "com.mitv.tvhome" || pkg == "com.xiaomi.mitv.tvhome") {
            launchLauncher()
        } else if (pkg == "com.google.android.tvlauncher") {
            if (!cls.contains("InputsPanelActivity") && !cls.contains("inputs")) {
                launchLauncher()
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    private fun launchLauncher() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }
}
