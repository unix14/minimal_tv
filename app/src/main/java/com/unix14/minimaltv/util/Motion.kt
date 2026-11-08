package com.unix14.minimaltv.util

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat

/**
 * Central switch for the "Reduce animations" performance setting.
 * Stored in plain SharedPreferences (not DataStore) so every animation site can read it synchronously on the UI thread.
 * When enabled, all durations collapse to 0, decorative animations are skipped and card shadows (expensive on low-end TV GPUs) are turned off.
 */
object Motion {

    private const val PREFS = "minimal_tv_motion"
    private const val KEY_REDUCE = "reduce_animations"

    @Volatile
    var reduced: Boolean = false
        private set

    fun init(context: Context) {
        reduced = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REDUCE, false)
    }

    fun setReduced(context: Context, value: Boolean) {
        reduced = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_REDUCE, value).apply()
    }

    /** Returns [ms], or 0 when animations are reduced. */
    fun duration(ms: Long): Long = if (reduced) 0L else ms

    /** Applies a card focus scale + elevation, instantly and shadow-free when animations are reduced. */
    fun focusCard(v: View, scale: Float, elevation: Float, durationMs: Long = 150) {
        v.animate().cancel()
        if (reduced) {
            v.scaleX = scale
            v.scaleY = scale
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
