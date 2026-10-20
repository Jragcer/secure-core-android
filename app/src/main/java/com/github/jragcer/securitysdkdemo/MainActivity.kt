package com.github.jragcer.securitysdkdemo

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.storage.PassphraseManager
import com.github.jragcer.securitycore.storage.SecureFileStorage
import com.github.jragcer.securitycore.storage.SecurePreferences
import kotlinx.coroutines.launch

class MainActivity: ComponentActivity() {
    val tag = "SecureCoreDemo"
    val pepe = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /////////////////////////////////////////////////////////////////////////////
        Log.d(tag, "SecureCore Version: ${SecureCore.VERSION}")

        // Comprobar seguridad del hardware
        if (SecureCore.isStrongBoxSupported(applicationContext)) {
            Log.i(tag, "El dispositivo cuenta con chip StrongBox Keymaster dedicado.")
        }

        /////////////////////////////////////////////////////////////////////////////
        val passphraseManager = PassphraseManager(applicationContext)

        //Demo PassphraseManager
        passphraseManager.getOrCreatePassphrase(filename = "demo_db_passphrase.bin", length = 32)
            .onSuccess { Log.d(tag, "Passphrase generated/recovered successfully: ${it.size} bytes")}
            .onFailure { Log.e(tag, "Error in PassphraseManager", it) }

        //Demo SecurePreferences
        SecurePreferences.create(applicationContext, fileName = "demo_secure_prefs")
            .onSuccess {preferences ->
                preferences.putString("demo_auth_token", "demo_jwt_token_1234567")
                Log.d(tag, "Auth token saved: ${preferences.getString("demo_auth_token")}")
            }
            .onFailure { Log.e(tag, "Error in SecurePreferences", it) }

        //Demo SecureFileStorage
        val fileStorage = SecureFileStorage(applicationContext)

        lifecycleScope.launch {
            fileStorage.writeText("demo_confidential_note.txt", "Confidential note content")
                .onSuccess { Log.d(tag, "Secure file written successfully") }
                .onFailure { Log.e(tag, "Error writing file", it) }

            fileStorage.readText("demo_confidential_note.txt")
                .onSuccess {content -> Log.d(tag, "File read successfully: $content") }
                .onFailure { Log.e(tag, "Error reading file", it) }
        }

        /////////////////////////////////////////////////////////////////////////////

    }
}