package com.github.jragcer.securitycore.api.storage

import androidx.security.crypto.EncryptedFile
import java.io.File
import android.content.Context

/**
 *  Public interface for cryptographically secure passphrase generation and retrieval.
 */
interface PassphraseManager {

    /**
     *  Retrieves the passphrase persisted on disk or generates a new one in a cryptographically secure manner if it does not exist.
     *
     *  If the file already exists, it decrypts and returns its contents. If it does not exist, if the file is empty, or if data
     *  corruption is detected, it generates a new sequence of secure random bytes, stores it encrypted on disk atomically, and returns it.
     *
     *  Backed by [EncryptedFile] with AES256-GCM encryption supported by Android Keystore.
     *
     *  @param filename The name of the encrypted file that will contain the key. Defaults to `"passphrase.bin"`.
     *  @param length Key length in bytes. Defaults to `32` bytes (256 bits).
     *  @param directory Optional target directory. Falls back to [Context.getNoBackupFilesDir] if null.
     *  @return A [Result] encapsulating the decrypted [ByteArray] upon success,
     *         or the corresponding exception ([java.io.IOException], [java.security.GeneralSecurityException]) in the event of an error.
     */
    fun getOrCreatePassphrase(
        filename: String = "passphrase.bin",
        length: Int = 32,
        directory: File? = null): Result<ByteArray>
}