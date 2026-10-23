package com.github.jragcer.securitysdkdemo

import android.os.Bundle
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
import kotlinx.coroutines.launch

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
                                val base64Key = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
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
                        securePreferences?.putString("session_token", "encrypted_jwt_token_sample_12345")
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