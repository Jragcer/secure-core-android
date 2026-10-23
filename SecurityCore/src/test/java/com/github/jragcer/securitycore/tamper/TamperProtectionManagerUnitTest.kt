package com.github.jragcer.securitycore.tamper

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.github.jragcer.securitycore.api.tamper.TamperProtectionManager
import com.github.jragcer.securitycore.internal.tamper.TamperProtectionManagerImpl
import android.content.pm.Signature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.security.MessageDigest

class TamperProtectionManagerUnitTest {
    private lateinit var mockContext: Context
    private lateinit var mockAppContext: Context
    private lateinit var mockPackageManager: PackageManager
    private lateinit var tamperManager: TamperProtectionManager

    private val sampleCertBytes = "SampleCertificateBinaryContent".toByteArray(Charsets.UTF_8)
    private val expectedSha256Hex = MessageDigest.getInstance("SHA-256")
        .digest(sampleCertBytes)
        .joinToString("") { "%02X".format(it) }

    @Before
    fun setUp(){
        mockContext = mock()
        mockAppContext = mock()
        mockPackageManager = mock()

        whenever(mockContext.applicationContext).thenReturn(mockAppContext)
        whenever(mockAppContext.packageName).thenReturn("com.github.jragcer.securitysdkdemo")
        whenever(mockAppContext.packageManager).thenReturn(mockPackageManager)

        tamperManager = TamperProtectionManagerImpl(mockContext)
    }

    @Test
    fun verifySignature_returnsTrue_whenHashMatchesExactly(){
        setupMockSignature(sampleCertBytes)

        val isValid = tamperManager.verifySignature(expectedSha256Hex)
        assertTrue(isValid)
    }

    @Test
    fun verifySignature_returnsTrue_whenHashContainsColonsAndMixedCase(){
        setupMockSignature(sampleCertBytes)

        val formattedHashWithColons = expectedSha256Hex
            .chunked(2)
            .joinToString(":")
            .lowercase()
        val iValid = tamperManager.verifySignature(formattedHashWithColons)

        assertTrue("verifySignature must ignore case and colon separators", iValid)
    }

    @Test
    fun verifySignature_returnsFalse_whenHashDoesNotMatch() {
        setupMockSignature(sampleCertBytes)

        val isValid = tamperManager.verifySignature("00112233445566778899AABBCCDDEEFF00112233445566778899AABBCCDDEEFF")

        assertFalse(isValid)
    }

    @Test
    fun verifySignature_returnsFalse_whenHashIsBlank() {
        setupMockSignature(sampleCertBytes)

        assertFalse(tamperManager.verifySignature(""))
        assertFalse(tamperManager.verifySignature("     "))
    }

    @Test
    fun isInstalledFromTrustedStore_returnsTrue_whenInstallerIsInWhitelist(){
        whenever(mockPackageManager.getInstallerPackageName("com.github.jragcer.securitysdkdemo"))
            .thenReturn("com.android.vending")

        val isTrusted = tamperManager.isInstalledFromTrustedStore(setOf("com.android.vending"))

        assertTrue(isTrusted)
    }

    @Test
    fun isInstalledFromTrustedStore_returnsFalse_whenInstallerIsSideloaded(){
        whenever(mockPackageManager.getInstallerPackageName("com.github.jragcer.securitysdkdemo"))
            .thenReturn(null)

        val isTrusted = tamperManager.isInstalledFromTrustedStore()

        assertFalse(isTrusted)
    }

    @Test
    fun isInstalledFromTrustedStore_returnsFalse_whenInstallerIsNotInWhitelist(){
        whenever(mockPackageManager.getInstallerPackageName("com.github.jragcer.securitysdkdemo"))
            .thenReturn("com.untrusted.thirdpartystore")

        val isTrusted = tamperManager.isInstalledFromTrustedStore(setOf("com.android.vending"))

        assertFalse(isTrusted)
    }

    @Test
    fun getSignatureStatus_computesIsGenuineTrue_whenSignatureAndInstallerMatch(){
        setupMockSignature(sampleCertBytes)
        setupMockInstaller("com.android.vending")

        val status = tamperManager.getSignatureStatus(
            expectedHash = expectedSha256Hex,
            trustedInstallers = setOf("com.android.vending")
        )

        assertTrue("Signature must be valid", status.isSignatureValid)
        assertTrue("Installer must be trusted", status.isInstallerTrusted)
        assertTrue("Status must be genuine when both pass", status.isGenuine)
        assertEquals(expectedSha256Hex, status.currentSignatureHash)
        assertEquals("com.android.vending", status.installerPackageName)
    }

    @Test
    fun getSignatureStatus_computesIsGenuineFalse_whenInstallerIsNotInTrustedList(){
        setupMockSignature(sampleCertBytes)
        setupMockInstaller("com.android.vending")

        val status = tamperManager.getSignatureStatus(
            expectedHash = expectedSha256Hex,
            trustedInstallers = setOf("com.other.store")
        )

        assertTrue("Signature should still be valid", status.isSignatureValid)
        assertFalse("Installer must not be trusted", status.isInstallerTrusted)
        assertFalse("isGenuine must be false when installer is untrusted", status.isGenuine)
    }

    @Suppress("DEPRECATION")
    private fun setupMockSignature(certBytes: ByteArray) {
        val mockSignature = mock<Signature>()
        whenever(mockSignature.toByteArray()).thenReturn(certBytes)

        val packageInfo = PackageInfo().apply {
            signatures = arrayOf(mockSignature)
        }

        whenever(
            mockPackageManager.getPackageInfo(
                eq("com.github.jragcer.securitysdkdemo"),
                any<Int>()
            )
        ).thenReturn(packageInfo)
    }

    @Suppress("DEPRECATION")
    private fun setupMockInstaller(installerPackage: String?) {
        // Mock for API < 30
        whenever(mockPackageManager.getInstallerPackageName("com.github.jragcer.securitysdkdemo"))
            .thenReturn(installerPackage)

        // Mock for API >= 30 (Android R+)
        try {
            val mockInstallSourceInfo = mock<android.content.pm.InstallSourceInfo>()
            whenever(mockInstallSourceInfo.installingPackageName).thenReturn(installerPackage)
            whenever(mockPackageManager.getInstallSourceInfo("com.github.jragcer.securitysdkdemo"))
                .thenReturn(mockInstallSourceInfo)
        } catch (_: Throwable) {
        }
    }
}