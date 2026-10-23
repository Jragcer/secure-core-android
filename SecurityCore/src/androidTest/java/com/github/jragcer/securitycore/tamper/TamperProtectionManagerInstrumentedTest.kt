package com.github.jragcer.securitycore.tamper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.tamper.TamperProtectionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TamperProtectionManagerInstrumentedTest {
    private lateinit var targetContext: Context
    private lateinit var tamperManager: TamperProtectionManager

    @Before
    fun setUp(){
        targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        tamperManager = SecureCore.createTamperProtectionManager(targetContext)
    }

    @Test
    fun createTamperProtectionManager_returnsNonNullInstance(){
        assertNotNull(tamperManager)
    }

    @Test
    fun getAppSignatureHash_returnsValidSha256HexString(){
        val hash = tamperManager.getAppSignatureHash()
        assertNotNull(hash)
        assertTrue("The hash must not be empty in an installed APK", hash.isNotEmpty())
        assertEquals("A SHA-256 digest in hexadecimal must be 64 characters long.", 64, hash.length)
        assertTrue("The hash must consist only of hexadecimal characters.",hash.matches(Regex("^[0-9A-F]+$")))
    }

    @Test
    fun verifySignature_matchesOwnComputedHash(){
        val currentHash = tamperManager.getAppSignatureHash()
        assertTrue(tamperManager.verifySignature(currentHash))
    }

    @Test
    fun verifySignature_failsOnTamperedHash(){
        assertFalse(tamperManager.verifySignature("00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF"))
    }

    @Test
    fun getSignatureStatus_returnsCoherentState(){
        val currentHash = tamperManager.getAppSignatureHash()
        val status = tamperManager.getSignatureStatus(
            expectedHash = currentHash
        )

        assertNotNull(status)
        assertTrue(status.isSignatureValid)
        assertEquals(currentHash, status.currentSignatureHash)
    }
}