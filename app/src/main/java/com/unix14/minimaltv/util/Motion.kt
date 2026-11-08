package com.unix14.minimaltv.util

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceManager

/**
 * Central switch for the "Reduce animations" performance setting.
 * Stored in plain SharedPreferences (not DataStore) so every animation site can read it synchronously on the UI thread.
 * When enabled, all durations collapse to 0, decorative animations are skipped, card shadows are turned off,
 * and system animation scales (window, transition, animator) are forced to 0.0f.
 */
object Motion {

    const val KEY_REDUCE = "reduce_animations"

    @Volatile
    var reduced: Boolean = false
        private set

    fun init(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        reduced = prefs.getBoolean(KEY_REDUCE, false)
        if (reduced) {
            applySystemAnimationScale(context, 0.0f)
        }
    }

    fun setReduced(context: Context, value: Boolean) {
        reduced = value
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(KEY_REDUCE, value).apply()
        applySystemAnimationScale(context, if (value) 0.0f else 1.0f)
    }

    fun applySystemAnimationScale(context: Context, scale: Float) {
        try {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, scale)
            Settings.Global.putFloat(context.contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, scale)
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
        } catch (e: Exception) {
            android.util.Log.w("Motion", "Could not set system animation scale: ${e.message}")
        }
    }

    /** Returns [ms], or 0 when animations are reduced. */
    fun duration(ms: Long): Long = if (reduced) 0L else ms

    /** Applies a card focus scale + elevation, or flat zero-effect scale & shadows when animations are reduced. */
    fun focusCard(v: View, scale: Float, elevation: Float, durationMs: Long = 150) {
        v.animate().cancel()
        if (reduced) {
            v.scaleX = 1.0f
            v.scaleY = 1.0f
            ViewCompat.setElevation(v, 0f)
        } else {
            v.animate().scaleX(scale).scaleY(scale).setDuration(durationMs).setInterpolator(DecelerateInterpolator(2.0f)).start()
            ViewCompat.setElevation(v, elevation)
        }
    }

    /** Sets a static elevation, or none when animations are reduced. */
    fun elevation(v: View, elevation: Float) {
        ViewCompat.setElevation(v, if (reduced) 0f else elevation)
    }

    /** Click pulse + scale-up launch transition for [v]; when reduced, no pulse and an instant (no-animation) activity switch. */
    fun launchOptions(v: View, restingScale: Float): Bundle? {
        if (reduced) {
            v.scaleX = 1.0f
            v.scaleY = 1.0f
            v.alpha = 1.0f
            return try {
                ActivityOptionsCompat.makeCustomAnimation(v.context, 0, 0).toBundle()
            } catch (e: Exception) {
                null
            }
        }
        v.animate().scaleX(1.12f).scaleY(1.12f).alpha(0.85f).setDuration(120).withEndAction {
            v.scaleX = restingScale
            v.scaleY = restingScale
            v.alpha = 1.0f
        }.start()
        return try {
            ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.width, v.height).toBundle()
        } catch (e: Exception) {
            null
        }
    }
}
