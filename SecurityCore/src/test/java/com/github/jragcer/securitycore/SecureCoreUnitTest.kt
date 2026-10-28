package com.github.jragcer.securitycore

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SecureCoreUnitTest {
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
    }

    @Test
    fun getVersion_returnsCorrectSdkConstant() {
        assertEquals(SecureCore.VERSION, SecureCore.getVersion())
    }

    @Test
    fun getVersionName_returnsFormattedDescriptor() {
        val expected = "${SecureCore.LIBRARY} v${SecureCore.VERSION}"
        assertEquals(expected, SecureCore.getVersionName())
    }

    @Test
    fun isStrongBoxSupported_returnsFalse_onHostJvm() {
        // En entorno de tests de host JVM, Build.VERSION.SDK_INT es 0 (< API 28)
        assertFalse(SecureCore.isStrongBoxSupported(mockContext))
    }

    @Test
    fun createScreenProtectionManager_returnsNonNullInstance() {
        // ScreenProtectionManagerImpl no requiere contexto ni AndroidKeyStore
        val manager = SecureCore.createScreenProtectionManager()
        assertEquals(true, manager != null)
    }
}