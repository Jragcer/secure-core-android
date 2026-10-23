package com.github.jragcer.securitycore.screen

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager
import com.github.jragcer.securitycore.api.screen.SecureScreenEffect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenProtectionManagerInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var screenManager: ScreenProtectionManager

    @Before
    fun setUp() {
        screenManager = SecureCore.createScreenProtectionManager()
    }

    @Test
    fun createScreenProtectionManager_returnsNonNullInstance() {
        assertNotNull(screenManager)
    }

    @Test
    fun enableAndDisableProtection_updatesRealActivityWindowFlags() {
        var hostActivity: Activity? = null

        composeTestRule.setContent {
            hostActivity = LocalContext.current as? Activity
        }
        composeTestRule.waitForIdle()

        val activity = checkNotNull(hostActivity) { "Activity should not be null" }

        composeTestRule.runOnUiThread {
            screenManager.enableProtection(activity)
        }
        composeTestRule.waitForIdle()

        assertTrue(screenManager.isProtectionActive(activity))
        assertTrue((activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0)

        composeTestRule.runOnUiThread {
            screenManager.disableProtection(activity)
        }
        composeTestRule.waitForIdle()

        assertFalse(screenManager.isProtectionActive(activity))
        assertFalse((activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0)
    }

    @Test
    fun secureScreenEffect_enablesProtectionDuringComposition_andRestoresOnDispose() {
        var hostActivity: Activity? = null
        val showEffect = mutableStateOf(false)
        var callbackReceivedState: Boolean? = null

        composeTestRule.setContent {
            hostActivity = LocalContext.current as? Activity
            if (showEffect.value) {
                SecureScreenEffect(
                    enabled = true,
                    onProtectionChanged = { active -> callbackReceivedState = active }
                )
            }
        }
        composeTestRule.waitForIdle()

        val activity = checkNotNull(hostActivity) { "Activity should not be null" }

        // 1. Inicialmente no está montado el efecto
        assertFalse(screenManager.isProtectionActive(activity))

        // 2. Montamos el efecto Composable
        composeTestRule.runOnUiThread {
            showEffect.value = true
        }
        composeTestRule.waitForIdle()

        assertTrue(screenManager.isProtectionActive(activity))
        assertTrue(callbackReceivedState == true)

        // 3. Desmontamos el efecto Composable
        composeTestRule.runOnUiThread {
            showEffect.value = false
        }
        composeTestRule.waitForIdle()

        assertFalse(screenManager.isProtectionActive(activity))
        assertTrue(callbackReceivedState == false)
    }
}