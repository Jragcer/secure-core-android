package com.github.jragcer.securitycore.storage

import android.content.Context
import java.io.File
import java.security.SecureRandom
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey

/**
 * A secure manager for generating and persisting passphrases and master keys.
 *
 * It leverages the device's security hardware via [MasterKey] (Android Keystore / TEE)
 * in conjunction with [EncryptedFile], utilizing the AES256-GCM authenticated encryption scheme and HKDF key derivation.
 *
 * The generated keys are particularly well-suited for encrypting local databases (such as SQLCipher or Realm),
 * ensuring that null bytes (`0x00`) are excluded to prevent unexpected truncation within native libraries compiled in C/C++.
 *
 * @property context The application context used to interact with the Android Keystore and the file system.
 * @property storageDir The directory where the encrypted file will be stored. By default, it uses [Context.getNoBackupFilesDir]
 *                      to prevent keys from being inadvertently included in cloud backups (Google Cloud Backup).
 */
class PassphraseManager (
    private val context: Context,
    private val storageDir: File = context.noBackupFilesDir
){

    /**
     * Hardware-backed master key in the Android Keystore using the AES256-GCM scheme.
     */
    private val masterKey : MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Retrieves the passphrase persisted on disk or generates a new one in a cryptographically secure manner if it does not exist.
     *
     * If the file already exists, it decrypts and returns its contents. If it does not exist, if the file is empty, or if data
     * corruption is detected, it generates a new sequence of secure random bytes, stores it encrypted on disk atomically, and returns it.
     *
     * Backed by [EncryptedFile] with AES256-GCM encryption supported by Android Keystore.
     *
     * @param filename The name of the encrypted file that will contain the key. Defaults to `"passphrase.bin"`.
     * @param length Key length in bytes. Defaults to `32` bytes (256 bits).
     * @return A [Result] encapsulating the decrypted [ByteArray] upon success,
     *         or the corresponding exception ([java.io.IOException], [java.security.GeneralSecurityException]) in the event of an error.
     *
     */
    fun getOrCreatePassphrase(filename: String = "passphrase.bin",
                              length: Int = 32): Result<ByteArray> = runCatching {

        if(!storageDir.exists()){
            storageDir.mkdirs()
        }

        val file = File(storageDir, filename)

        if(file.exists() && file.length() > 0L){
            try {
                val encryptedFile = createEncryptedFile(file)
                encryptedFile.openFileInput().use { it.readBytes() }
            }catch (e: Exception){
                writeNewPassphrase(file, length)
            }
        }else {
            writeNewPassphrase(file, length)
        }
    }

    /**
     * Creates and configures an [EncryptedFile] instance tied to the Android Keystore MasterKey
     * using the AES-256 GCM HKDF 4KB encryption scheme.
     *
     * @param file The target file descriptor for encrypted reading or writing.
     * @return A configured [EncryptedFile] instance.
     */
    private fun createEncryptedFile(file: File): EncryptedFile {
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }

    /**
    * Generates a high-entropy cryptographic key and persists it encrypted on disk.
    *
    * If the physical file already exists (e.g., corrupted or 0-byte file), it is deleted
    * prior to writing, as [EncryptedFile] prevents overwriting existing files directly.
    *
    * @param file Target file descriptor where the key will be saved.
    * @param length Byte length of the passphrase to generate.
    * @return The generated and persisted [ByteArray] key.
     */
    private fun writeNewPassphrase(file: File, length: Int): ByteArray {
        if (file.exists()) {
            file.delete()
        }

        val passphrase = generateSafePassphrase(length)
        val encryptedFile = createEncryptedFile(file)

        encryptedFile.openFileOutput().use { output ->
            output.write(passphrase)
            output.flush()
        }

        return passphrase
    }

    /**
     * Generates a sequence of cryptographically secure random bytes using [SecureRandom].
     *
     * Filters out and ignores any byte with a value of `0x00` (null byte), avoiding string termination
     * issues ('\0') in native cryptographic engines such as SQLCipher.
     *
     * @param length The number of bytes required for the key.
     * @return An array of cryptographically secure bytes containing no null bytes.
      */
    fun generateSafePassphrase(length: Int): ByteArray {
        val random = SecureRandom()
        val buffer = ByteArray(length)

        for (i in 0 until length) {
            var b: Byte
            do {
                b= random.nextInt(256).toByte()

            }while (b == 0.toByte())
            buffer[i] = b
        }
        return buffer
    }
}