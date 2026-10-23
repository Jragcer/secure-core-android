package com.github.jragcer.securitycore.api.screen

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.github.jragcer.securitycore.SecureCore

@Composable
fun SecureScreenEffect (enabled:Boolean = true,
                        onProtectionChanged: ((Boolean) -> Unit)? = null){
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val manager = SecureCore.createScreenProtectionManager()

    DisposableEffect(enabled, activity){
        val wasInitiallyProtected = manager.isProtectionActive(activity)
        if(enabled){
            manager.enableProtection(activity)
            onProtectionChanged?.invoke(true)
        } else {
            manager.disableProtection(activity)
            onProtectionChanged?.invoke(false)
        }
        onDispose {
            if(!wasInitiallyProtected) {
                manager.disableProtection(activity)
                onProtectionChanged?.invoke(false)
            }else{
                onProtectionChanged?.invoke(true)
            }
        }
    }
}