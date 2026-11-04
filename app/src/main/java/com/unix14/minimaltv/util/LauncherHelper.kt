package com.unix14.minimaltv.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import com.unix14.minimaltv.R

object LauncherHelper {

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultPackage = resolveInfo?.activityInfo?.packageName
        return defaultPackage == context.packageName
    }

    fun openSetDefaultLauncher(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (!roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                        return
                    } catch (e: Exception) {}
                }
            }
        }

        try {
            val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.queryIntentActivities(homeSettingsIntent, 0).isNotEmpty()) {
                context.startActivity(homeSettingsIntent)
                return
            }
        } catch (e: Exception) {}

        try {
            val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (context.packageManager.queryIntentActivities(defaultAppsIntent, 0).isNotEmpty()) {
                context.startActivity(defaultAppsIntent)
                return
            }
        } catch (e: Exception) {}

        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(homeIntent, context.getString(R.string.settings_default_launcher_button)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            return
        } catch (e: Exception) {}

        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {}
    }

    fun isTvInputKey(keyCode: Int): Boolean {
        return keyCode == android.view.KeyEvent.KEYCODE_TV_INPUT || keyCode == android.view.KeyEvent.KEYCODE_WINDOW || keyCode in android.view.KeyEvent.KEYCODE_TV_INPUT_HDMI_1..android.view.KeyEvent.KEYCODE_TV_INPUT_VGA_1
    }

    /** Opens the MinimalTV / Android TV side drawer selection of HDMI inputs. */
    fun openInputSource(context: Context) {
        if (context is com.unix14.minimaltv.ui.main.MainActivity) {
            if (context.isInputsOpen) context.closeInputsDrawer() else context.showInputsDrawer()
            return
        }
        try {
            val intent = Intent(context, com.unix14.minimaltv.ui.inputs.InputsPanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("LauncherHelper", "Failed to start InputsPanelActivity", e)
        }
    }
}
