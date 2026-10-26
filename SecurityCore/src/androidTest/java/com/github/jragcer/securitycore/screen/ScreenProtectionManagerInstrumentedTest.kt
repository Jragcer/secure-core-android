package com.github.jragcer.securitycore.screen

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager
import com.github.jragcer.securitycore.api.screen.SecureScreenEffect
import org.junit.Assert.assertEquals
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
    val activityScenarioRule = ActivityScenarioRule(ComponentActivity::class.java)

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
        activityScenarioRule.scenario.onActivity { activity ->
            // 1. Estado inicial limpio
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            assertFalse(screenManager.isProtectionActive(activity))

            // 2. Activar protección
            screenManager.enableProtection(activity)
            assertTrue(screenManager.isProtectionActive(activity))
            assertTrue(
                (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
            )

            // 3. Desactivar protección
            screenManager.disableProtection(activity)
            assertFalse(screenManager.isProtectionActive(activity))
            assertEquals(
                0,
                activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE
            )
        }
    }

    @Test
    fun secureScreenEffect_enablesProtectionDuringComposition_andRestoresOnDispose() {
        val showEffect = mutableStateOf(false)
        var callbackReceivedState: Boolean? = null
        var targetComposeView: ComposeView? = null

        // 1. Montamos el ComposeView dentro de la Activity
        activityScenarioRule.scenario.onActivity { activity ->
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

            val composeView = ComposeView(activity).apply {
                setContent {
                    if (showEffect.value) {
                        SecureScreenEffect(
                            enabled = true,
                            onProtectionChanged = { active -> callbackReceivedState = active }
                        )
                    }
                }
            }
            targetComposeView = composeView
            activity.setContentView(composeView)
        }

        // Estado inicial (composable no montado)
        activityScenarioRule.scenario.onActivity { activity ->
            assertFalse(screenManager.isProtectionActive(activity))
        }

        // 2. Activamos el Composable y forzamos la recomposición en el hilo UI
        activityScenarioRule.scenario.onActivity {
            showEffect.value = true
            // Fuerza la invalidación y el pase de layout/dibujo en la vista de Compose
            targetComposeView?.requestLayout()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        // Si la recomposición sigue pendiente por el frame clock, forzamos un pulso en el UI thread
        activityScenarioRule.scenario.onActivity { activity ->
            targetComposeView?.let { view ->
                view.invalidate()
            }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        activityScenarioRule.scenario.onActivity { activity ->
            assertTrue(
                "FLAG_SECURE debe activarse al entrar en composición",
                screenManager.isProtectionActive(activity)
            )
            assertEquals(true, callbackReceivedState)
        }

        // 3. Desmontamos el Composable
        activityScenarioRule.scenario.onActivity {
            showEffect.value = false
            targetComposeView?.requestLayout()
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        activityScenarioRule.scenario.onActivity { activity ->
            assertFalse(
                "FLAG_SECURE debe eliminarse tras onDispose",
                screenManager.isProtectionActive(activity)
            )
            assertEquals(false, callbackReceivedState)
        }
    }
}