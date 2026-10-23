package com.github.jragcer.securitycore.internal.screen

import android.app.Activity
import android.view.WindowManager
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager

/**
 * Internal implementation of [ScreenProtectionManager] managing [WindowManager.LayoutParams.FLAG_SECURE]
 * on target window instances.
 */
internal class ScreenProtectionManagerImpl : ScreenProtectionManager {
    /**
     * Enables screen protection on the specified [Activity] by applying [android.view.WindowManager.LayoutParams.FLAG_SECURE].
     * Blocks screenshots, video recording, and obscures content in the recent apps preview.
     *
     * @param activity The target activity to protect.
     */
    override fun enableProtection(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    }

    /**
     * Disables screen protection on the specified [Activity] by removing [android.view.WindowManager.LayoutParams.FLAG_SECURE].
     *
     * @param activity The target activity to unprotect.
     */
    override fun disableProtection(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Toggles screen protection on the specified [Activity].
     *
     * @param activity The target activity.
     * @return True if protection is now active, false if disabled.
     */
    override fun toggleProtection(activity: Activity): Boolean {
        return if(isProtectionActive(activity)){
            disableProtection(activity)
            false
        } else {
            enableProtection(activity)
            true
        }
    }

    /**
     * Checks whether screen protection is currently active on the given [Activity].
     *
     * @param activity The activity to inspect.
     * @return True if [android.view.WindowManager.LayoutParams.FLAG_SECURE] is present in window flags.
     */
    override fun isProtectionActive(activity: Activity): Boolean {
        val flags = activity.window.attributes.flags
        return (flags and WindowManager.LayoutParams.FLAG_SECURE ) != 0
    }
}