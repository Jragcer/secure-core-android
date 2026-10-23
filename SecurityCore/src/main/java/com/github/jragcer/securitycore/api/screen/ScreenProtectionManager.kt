package com.github.jragcer.securitycore.api.screen

import android.app.Activity

/**
 * Public interface providing runtime screen protection mechanisms to prevent
 * unauthorized screenshots, screen recording, and exposure in the Android App Switcher (Recents).
 */
interface ScreenProtectionManager {
    /**
     * Enables screen protection on the specified [Activity] by applying [android.view.WindowManager.LayoutParams.FLAG_SECURE].
     * Blocks screenshots, video recording, and obscures content in the recent apps preview.
     *
     * @param activity The target activity to protect.
     */
    fun enableProtection(activity: Activity)

    /**
     * Disables screen protection on the specified [Activity] by removing [android.view.WindowManager.LayoutParams.FLAG_SECURE].
     *
     * @param activity The target activity to unprotect.
     */
    fun disableProtection(activity: Activity)

    /**
     * Toggles screen protection on the specified [Activity].
     *
     * @param activity The target activity.
     * @return True if protection is now active, false if disabled.
     */
    fun toggleProtection(activity: Activity): Boolean

    /**
     * Checks whether screen protection is currently active on the given [Activity].
     *
     * @param activity The activity to inspect.
     * @return True if [android.view.WindowManager.LayoutParams.FLAG_SECURE] is present in window flags.
     */
    fun isProtectionActive(activity: Activity): Boolean
}