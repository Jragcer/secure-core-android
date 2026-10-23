package com.github.jragcer.securitycore.screen

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager
import com.github.jragcer.securitycore.internal.screen.ScreenProtectionManagerImpl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ScreenProtectionManagerUnitTest {
    private lateinit var mockActivity: Activity
    private lateinit var mockWindow: Window
    private lateinit var windowAttributes: WindowManager.LayoutParams
    private lateinit var screenManager: ScreenProtectionManager

    @Before
    fun setUp(){
        mockActivity = mock()
        mockWindow = mock()
        windowAttributes = WindowManager.LayoutParams()

        whenever(mockActivity.window).thenReturn(mockWindow)
        whenever(mockWindow.attributes).thenReturn(windowAttributes)

        screenManager = ScreenProtectionManagerImpl()
    }

    @Test
    fun enableProtection_setsFlagSecureOnWindow() {
        screenManager.enableProtection(mockActivity)

        verify(mockWindow).addFlags(eq(WindowManager.LayoutParams.FLAG_SECURE))
    }

    @Test
    fun disableProtection_clearsFlagSecureOnWindow() {
        screenManager.disableProtection(mockActivity)

        verify(mockWindow).clearFlags(eq(WindowManager.LayoutParams.FLAG_SECURE))
    }

    @Test
    fun isProtectionActive_returnsTrue_whenFlagIsPresent() {
        windowAttributes.flags = windowAttributes.flags or WindowManager.LayoutParams.FLAG_SECURE

        val isActive = screenManager.isProtectionActive(mockActivity)

        assertTrue(isActive)
    }

    @Test
    fun isProtectionActive_returnsFalse_whenFlagIsNotPresent() {
        windowAttributes.flags = 0

        val isActive = screenManager.isProtectionActive(mockActivity)

        assertFalse(isActive)
    }

    @Test
    fun toggleProtection_enablesWhenCurrentlyInactive() {
        windowAttributes.flags = 0

        val newState = screenManager.toggleProtection(mockActivity)

        assertTrue(newState)
        verify(mockWindow).addFlags(eq(WindowManager.LayoutParams.FLAG_SECURE))
    }

    @Test
    fun toggleProtection_disablesWhenCurrentlyActive() {
        windowAttributes.flags = windowAttributes.flags or WindowManager.LayoutParams.FLAG_SECURE

        val newState = screenManager.toggleProtection(mockActivity)

        assertFalse(newState)
        verify(mockWindow).clearFlags(eq(WindowManager.LayoutParams.FLAG_SECURE))
    }
}