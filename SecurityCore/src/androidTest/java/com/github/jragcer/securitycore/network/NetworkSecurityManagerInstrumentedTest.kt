package com.github.jragcer.securitycore.network

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.api.network.NetworkSecurityManager
import com.github.jragcer.securitycore.internal.network.NetworkSecurityManagerImpl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLPeerUnverifiedException

@RunWith(AndroidJUnit4::class)
class NetworkSecurityManagerInstrumentedTest {

    private lateinit var networkManager: NetworkSecurityManager

    @Before
    fun setUp(){
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        networkManager = NetworkSecurityManagerImpl(context)
    }

    @Test
    fun createNetworkSecurityManager_returnsNonNullInstance() {
        assertNotNull(networkManager)
    }

    @Test
    fun hasUserInstalledCertificates_returnsFalseOnCleanDeviceEnvironment() {
        // En emuladores o dispositivos estándar de pruebas no debe haber CAs de usuario
        val hasUserCAs = networkManager.hasUserInstalledCertificates()
        assertFalse(hasUserCAs)
    }

    @Test
    fun applyPinning_rejectsConnectionWithMismatchedPin_simulatingMitmBlock() {
        val host = "api.github.com"
        val invalidPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

        val client = networkManager.applyPinning(
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS),
            mapOf(host to listOf(invalidPin))
        ).build()

        val request = Request.Builder()
            .url("https://$host")
            .build()

        try {
            client.newCall(request).execute().use {
                fail("La petición debió fallar por SSLPeerUnverifiedException")
            }
        } catch (e: SSLPeerUnverifiedException) {
            // El bloqueo funcionó como se esperaba
            val message = e.message.orEmpty()
            assertTrue(
                "El error debe especificar el rechazo del certificado",
                message.contains("Certificate pinning failure") || message.contains("api.github.com")
            )
        }
    }

    @Test
    fun applyPinning_allowsConnectionWhenPinMatchesServerKey() {
        val host = "api.github.com"
        val dummyPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

        val discoveryClient = networkManager.applyPinning(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS),
            mapOf(host to listOf(dummyPin))
        ).build()

        val pinRegex = Regex("""sha256/([A-Za-z0-9+/]{43}=)""")

        val validPin = try {
            discoveryClient.newCall(Request.Builder().url("https://$host").build()).execute()
            null
        } catch (e: SSLPeerUnverifiedException) {
            val message = e.message.orEmpty()
            pinRegex.find(message)?.value
        }

        assertNotNull("Debe extraerse un hash SHA-256 Base64 válido de la respuesta de OkHttp", validPin)

        // Verificamos conexión exitosa usando el pin extraído
        val validClient = networkManager.applyPinning(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS),
            mapOf(host to listOf(validPin!!))
        ).build()

        val request = Request.Builder().url("https://$host").build()
        validClient.newCall(request).execute().use { response ->
            assertTrue("La conexión debe completarse exitosamente con el pin legítimo", response.isSuccessful || response.code < 500)
        }
    }
}