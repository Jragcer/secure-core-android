package com.github.jragcer.securitysdkdemo

import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.biometric.BiometricResult
import com.github.jragcer.securitycore.api.biometric.BiometricStatus
import com.github.jragcer.securitysdkdemo.components.ExpandableSectionCard
import com.github.jragcer.securitysdkdemo.components.SensitiveDataModal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.net.ssl.SSLPeerUnverifiedException

class MainActivity: FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme{
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecurityDemoScreen(activity = this)
                }
            }
        }
    }

    @Composable
    fun SecurityDemoScreen(activity: FragmentActivity){
        val context = activity.applicationContext
        val scope = rememberCoroutineScope()
        var statusText by rememberSaveable { mutableStateOf("Ready to test SecurityCore SDK.") }

        val biometricManager = remember { SecureCore.createBiometricAuthManager(context) }
        val passphraseManager = remember { SecureCore.createPassphraseManager(context) }
        val securePreferences = remember { SecureCore.createSecurePreferences(context).getOrNull() }
        val secureFileStorage = remember { SecureCore.createSecureFileStorage(context) }
        val integrityManager = remember { SecureCore.createIntegrityManager(context) }
        val tamperProtectionManager = remember { SecureCore.createTamperProtectionManager(context) }
        val screenProtectionManager = remember { SecureCore.createScreenProtectionManager() }
        val networkSecurityManager = remember { SecureCore.createNetworkSecurityManager(context) }
        val cryptoManager = remember { SecureCore.createCryptoManager("DemoMasterKey") }

        var isScreenProtected by remember {
            mutableStateOf(screenProtectionManager.isProtectionActive(activity))
        }
        var showSensitiveDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            if (SecureCore.isStrongBoxSupported(applicationContext)) {
                statusText += "\nThe device features a dedicated StrongBox Keymaster chip."
            }
        }

        Column (modifier = Modifier.fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)){

            Text(
                text = SecureCore.getVersionName(),
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Console Output:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            ExpandableSectionCard(
                title = "Hardware-backed Crypto (AES-256-GCM)",
                initiallyExpanded = false
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        try {
                            val samplePayload = "Confidential Payload - ${System.currentTimeMillis()}"
                            val rawBytes = samplePayload.toByteArray(Charsets.UTF_8)

                            // 1. Encrypt arbitrary ByteArray
                            val encryptedBytes = cryptoManager.encrypt(rawBytes)

                            // 2. Decrypt back and verify 128-bit GCM tag
                            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
                            val recoveredText = String(decryptedBytes, Charsets.UTF_8)

                            val summary = buildString {
                                appendLine("Keystore AES-GCM Status:")
                                appendLine(" • Original: $samplePayload")
                                appendLine(" • Ciphertext Size: ${encryptedBytes.size} bytes (IV + Tag included)")
                                appendLine(" • Decrypted: $recoveredText")
                                appendLine(" • Integrity Verified: ${samplePayload == recoveredText}")
                            }
                            statusText = summary.trimEnd()
                        } catch (e: Exception) {
                            statusText = "Crypto Error: ${e.message}"
                        }
                    }
                ) {
                    Text("Test KeyStore Encrypt & Decrypt")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        try {
                            // Generate a 64 KB test payload for stream verification
                            val streamPayload = ByteArray(65536) { (it % 255).toByte() }
                            val inputStream = java.io.ByteArrayInputStream(streamPayload)
                            val encryptedOut = java.io.ByteArrayOutputStream()

                            // Stream encryption without holding entire pipeline in RAM
                            cryptoManager.encrypt(inputStream, encryptedOut)
                            val cipherBytes = encryptedOut.toByteArray()

                            // Stream decryption
                            val cipherIn = java.io.ByteArrayInputStream(cipherBytes)
                            val decryptedOut = java.io.ByteArrayOutputStream()
                            cryptoManager.decrypt(cipherIn, decryptedOut)

                            val recoveredBytes = decryptedOut.toByteArray()
                            val isDataIdentical = streamPayload.contentEquals(recoveredBytes)

                            statusText = buildString {
                                appendLine("Stream Processing Result:")
                                appendLine(" • Payload Size: ${streamPayload.size / 1024} KB")
                                appendLine(" • Cipher Stream Size: ${cipherBytes.size} bytes")
                                appendLine(" • Stream Round-trip: ${if (isDataIdentical) "SUCCESS (Match)" else "FAILURE (Corrupted)"}")
                            }.trimEnd()
                        } catch (e: Exception) {
                            statusText = "Streaming Error: ${e.message}"
                        }
                    }
                ) {
                    Text("Test 64 KB Stream Encryption")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "Network Security & SSL Pinning",
                initiallyExpanded = false
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val hasUserCAs = networkSecurityManager.hasUserInstalledCertificates()
                        val summary = buildString {
                            appendLine("TrustStore Inspection:")
                            appendLine(" • User CAs Detected: ${if (hasUserCAs) "YES (Risk: MitM / Proxy active)" else "NO (Clean system store)"}")
                            appendLine(" • Potential Proxy/Packet Capture Tool Active: $hasUserCAs")
                        }
                        statusText = summary.trimEnd()
                    }
                ) {
                    Text("Check User-Installed CAs (Proxy Check)")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val host = "api.github.com"
                            val wrongPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
                            val clientBuilder = OkHttpClient.Builder()
                            val pins = mapOf(host to listOf(wrongPin))

                            statusText = withContext(Dispatchers.IO) {
                                try {
                                    val client = networkSecurityManager.applyPinning(clientBuilder, pins).build()
                                    val request = Request.Builder().url("https://$host").build()
                                    client.newCall(request).execute().use { response ->
                                        "Unexpected: Connection succeeded with HTTP ${response.code}"
                                    }
                                } catch (e: SSLPeerUnverifiedException) {
                                    buildString {
                                        appendLine("Pinning Verification:")
                                        appendLine(" • Connection Blocked: SUCCESS")
                                        appendLine(" • Reason: SSLPeerUnverifiedException")
                                        appendLine(" • MitM Attack Prevented: Host key rejected")
                                    }.trimEnd()
                                } catch (e: Exception) {
                                    "Network Error: ${e.message}"
                                }
                            }
                        }
                    }
                ) {
                    Text("Test Invalid Pin (Simulate MitM Block)")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val host = "api.github.com"
                            val dummyPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
                            val clientBuilder = OkHttpClient.Builder()
                            val pins = mapOf(host to listOf(dummyPin))

                            statusText = withContext(Dispatchers.IO) {
                                try {
                                    val client = networkSecurityManager.applyPinning(clientBuilder, pins).build()
                                    val request = Request.Builder().url("https://$host").build()
                                    client.newCall(request).execute().use { }
                                    "Connected without pinning error."
                                } catch (e: SSLPeerUnverifiedException) {
                                    val extractedPins = e.message
                                        ?.lines()
                                        ?.filter { it.contains("sha256/") }
                                        ?.map { " • ${it.trim().removePrefix("Pin: ")}" }
                                        ?.joinToString("\n") ?: "No pins discovered"

                                    buildString {
                                        appendLine("Host Public Key Pins ($host):")
                                        appendLine(extractedPins)
                                    }.trimEnd()
                                } catch (e: Exception) {
                                    "Error inspecting pins: ${e.message}"
                                }
                            }
                        }
                    }
                ) {
                    Text("Inspect Host Public Key Pins")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "Screen & Window Protection (FLAG_SECURE)",
                initiallyExpanded = false
            ) {
                Text(
                    text = "Current Status: ${if (isScreenProtected) "PROTECTED (FLAG_SECURE active)" else "UNPROTECTED"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isScreenProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val newState = screenProtectionManager.toggleProtection(activity)
                        isScreenProtected = newState
                        statusText = if (newState) {
                            "Screen Protection: ENABLED\n" +
                                    " • Screenshots blocked\n" +
                                    " • Video recordings blocked\n" +
                                    " • Obscured in App Switcher (Recents)"
                        } else {
                            "Screen Protection: DISABLED\n" +
                                    " • Window flags cleared (Normal mode)"
                        }
                    }
                ) {
                    Text(if (isScreenProtected) "Disable Screen Protection" else "Enable Screen Protection")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val active = screenProtectionManager.isProtectionActive(activity)
                        isScreenProtected = active
                        statusText = "Screen Protection Check:\n" +
                                " • FLAG_SECURE Present: $active\n" +
                                " • Window: ${activity.window}"
                    }
                ) {
                    Text("Verify Window Security Flags")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showSensitiveDialog = true }
                ) {
                    Text("Open Screen with SecureScreenEffect")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "APK Tamper & Signature Protection",
                initiallyExpanded = false
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val currentHash = tamperProtectionManager.getAppSignatureHash()
                        val status = tamperProtectionManager.getSignatureStatus(
                            expectedHash = currentHash
                        )

                        val summary = buildString {
                            appendLine("Tamper Status:")
                            appendLine(" • Genuine State: ${if (status.isGenuine) "GENUINE" else "UNTRUSTED / SIDELOADED"}")
                            appendLine(" • Signature Valid: ${status.isSignatureValid}")
                            appendLine(" • Installer Trusted: ${status.isInstallerTrusted}")
                            appendLine(" • Installer Source: ${status.installerPackageName ?: "Sideloaded / Unknown (Debug)"}")
                            appendLine(" • Active Hash: ${status.currentSignatureHash.ifEmpty { "Unavailable" }}")
                        }
                        statusText = summary.trimEnd()
                    }
                ) {
                    Text("Verify APK Signature & Origin")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val currentHash = tamperProtectionManager.getAppSignatureHash()
                        statusText = if (currentHash.isNotEmpty()) {
                            "Current Signing SHA-256:\n$currentHash"
                        } else {
                            "Unable to read package signing certificates."
                        }
                    }
                ) {
                    Text("Read Signing Certificate Hash")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "Device & Runtime Integrity",
                initiallyExpanded = false
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val report = integrityManager.getIntegrityReport()
                        val summary = buildString {
                            appendLine("Integrity Status: ${if (report.isSecure) "SECURE" else "THREATS DETECTED"}")
                            if (report.detectedThreats.isNotEmpty()) {
                                appendLine("Threats: ${report.detectedThreats.joinToString(", ")}")
                            }
                            appendLine("Details:")
                            report.details.forEach { (check, detected) ->
                                appendLine(" - $check: $detected")
                            }
                        }
                        statusText = summary.trimEnd()
                    }
                ) {
                    Text("Run Integrity Assessment")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val isRooted = integrityManager.isRooted()
                        val isEmulator = integrityManager.isEmulator()
                        val isHooked = integrityManager.isDebuggerOrHookingActive()
                        val compromised = integrityManager.isDeviceCompromised()

                        statusText = "Fast Heuristics:\n" +
                                " • Device Compromised: $compromised\n" +
                                " • Root Detected: $isRooted\n" +
                                " • Emulator Detected: $isEmulator\n" +
                                " • Debugger/Hook Active: $isHooked"
                    }
                ) {
                    Text("Quick Heuristic Checks")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "Biometric Authentication",
                initiallyExpanded = false
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val status = biometricManager.canAuthenticate(allowDeviceCredential = true)
                        if (status is BiometricStatus.Ready) {
                            biometricManager.authenticate(
                                activity = activity,
                                title = "Biometric Verification",
                                subtitle = "Authenticate to unlock secure operations",
                                allowDeviceCredential = true
                            ) { result ->
                                statusText = when (result) {
                                    is BiometricResult.Success -> {
                                        Toast.makeText(
                                            activity,
                                            "Authentication successful!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        "Biometric Status: SUCCESS"
                                    }

                                    is BiometricResult.Error -> {
                                        "Biometric Error [${result.errorCode}]: ${result.errorMessage}"
                                    }

                                    is BiometricResult.Failed -> {
                                        "Biometric Status: FAILED (Unrecognized biometric)"
                                    }
                                }
                            }
                        } else {
                            statusText = "Biometric unavailable: $status"
                            Toast.makeText(activity, "Status: $status", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Launch Biometric Prompt")
                }
            }

            // ==========================================
            ExpandableSectionCard(
                title = "Secure Storage Primitives",
                initiallyExpanded = false
            ) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        passphraseManager.getOrCreatePassphrase("demo_passphrase_key")
                            .onSuccess { bytes ->
                                val hexKey = bytes.joinToString(separator = "") { byte ->
                                    "%02x".format(byte)
                                }
                                val base64Key = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                statusText = "Passphrase loaded (${bytes.size} bytes):"
                                statusText += "\nHex->$hexKey"
                                statusText += "\nBase64->$base64Key"
                            }
                            .onFailure { error ->
                                statusText = "Passphrase error: ${error.message}"
                            }
                    }
                ) {
                    Text("Test PassphraseManager")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        securePreferences?.putString(
                            "session_token",
                            "encrypted_jwt_token_sample_12345"
                        )
                        val retrieved = securePreferences?.getString("session_token")
                        statusText = "SecurePreferences: stored & decrypted -> '$retrieved'"
                    }
                ) {
                    Text("Test SecurePreferences")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            val fileName = "demo_secure_note.txt"
                            val content = "Confidential payload secured with EncryptedFile."

                            secureFileStorage.writeText(fileName, content)
                                .onSuccess {
                                    val readBack = secureFileStorage.readText(fileName).getOrNull()
                                    statusText = "SecureFileStorage: '$readBack'"
                                }
                                .onFailure {
                                    statusText = "File write error: ${it.message}"
                                }
                        }
                    }
                ) {
                    Text("Test SecureFileStorage")
                }
            }
        }

        if (showSensitiveDialog) {
            SensitiveDataModal(
                onDismiss = {
                    showSensitiveDialog = false
                    // Actualizamos el estado visual para comprobar que se restauró
                    isScreenProtected = screenProtectionManager.isProtectionActive(activity)
                    statusText = "Left sensitive screen: Protection auto-restored."
                },
                onProtectionChanged = { active ->
                    isScreenProtected = active
                }
            )
        }
    }

    @Preview()
    @Composable
    fun MainScreenPreview() {
        MaterialTheme{
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SecurityDemoScreen(activity = this)
            }
        }
    }
}