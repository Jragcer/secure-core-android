package com.github.jragcer.securitycore.network

import android.content.Context
import com.github.jragcer.securitycore.api.network.NetworkSecurityManager
import com.github.jragcer.securitycore.internal.network.NetworkSecurityManagerImpl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class NetworkSecurityManagerUnitTest {
    private lateinit var mockContext: Context
    private lateinit var networkManager: NetworkSecurityManager

    @Before
    fun setUp(){
        mockContext = mock()
        networkManager = NetworkSecurityManagerImpl(mockContext)
    }

    @Test
    fun createCertificatePinner_formatsAndAppliesSha256PrefixAutomatically() {
        val rawHash = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val fullHash = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="

        val pins = mapOf(
            "api.example.com" to listOf(rawHash, fullHash)
        )

        val pinner = networkManager.createCertificatePinner(pins)

        assertNotNull(pinner)
        assertEquals(2, pinner.pins.size)

        val configuredPins = pinner.pins.map { it.hash }
        assertTrue(configuredPins.any { it.hex().isNotEmpty() })
    }

    @Test
    fun createCertificatePinner_handlesMultipleHostsCorrectly() {
        val pins = mapOf(
            "auth.domain.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="),
            "api.domain.com" to listOf("sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        )

        val pinner = networkManager.createCertificatePinner(pins)

        assertEquals(2, pinner.pins.size)
        val patterns = pinner.pins.map { it.pattern }
        assertTrue(patterns.contains("auth.domain.com"))
        assertTrue(patterns.contains("api.domain.com"))
    }

    @Test
    fun createCertificatePinner_withEmptyMap_returnsEmptyPinnerWithoutCrashing() {
        val pinner = networkManager.createCertificatePinner(emptyMap())

        assertNotNull(pinner)
        assertTrue(pinner.pins.isEmpty())
    }

    @Test
    fun createCertificatePinner_withEmptyHashListForHost_buildsCleanly() {
        val pins = mapOf("api.example.com" to emptyList<String>())
        val pinner = networkManager.createCertificatePinner(pins)

        assertNotNull(pinner)
        assertTrue(pinner.pins.isEmpty())
    }

    @Test
    fun createCertificatePinner_preservesWildcardDomainPatterns() {
        val wildcardHost = "*.github.com"
        val pin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

        val pinner = networkManager.createCertificatePinner(mapOf(wildcardHost to listOf(pin)))

        assertEquals(1, pinner.pins.size)
        assertEquals(wildcardHost, pinner.pins.first().pattern)
    }

    @Test
    fun hasUserInstalledCertificates_handlesMissingStoreGracefullyOnJvm() {
        // En entorno JVM puro, AndroidCAStore no está registrado por defecto
        val result = networkManager.hasUserInstalledCertificates()
        // No debe lanzar excepción no controlada, debe retornar false de forma resiliente
        assertFalse(result)
    }

    @Test
    fun applyPinning_configuresPinnerOnOkHttpClientBuilder() {
        val builder = OkHttpClient.Builder()
        val pins = mapOf(
            "test.secure.com" to listOf("sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        )

        val updatedBuilder = networkManager.applyPinning(builder, pins)
        val client = updatedBuilder.build()

        assertNotNull(client.certificatePinner)
        assertEquals(1, client.certificatePinner.pins.size)
        assertEquals("test.secure.com", client.certificatePinner.pins.first().pattern)
    }
}