package com.github.jragcer.securitycore.storage

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

/**
 * Manages transparent, hardware-backed encrypted file operations on local storage.
 *
 * This component leverages [EncryptedFile] backed by Android Keystore through a [MasterKey]
 * using the authenticated AES-256 GCM encryption scheme with HKDF key derivation
 * ([EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB]).
 *
 * All operations execute off the main thread using [Dispatchers.IO] to prevent UI stuttering
 * and return structured [Result] instances instead of leaking raw cryptographic or I/O exceptions.
 *
 * @property context The application context used to resolve internal directories and interact with the Keystore.
 * @property defaultDirectory The fallback directory where encrypted files reside. Defaults to [Context.getFilesDir].
 */
class SecureFileStorage (
    private val context: Context,
    private val defaultDirectory: File = context.filesDir
) {
    /**
     * Hardware-backed master key in the Android Keystore using the AES256-GCM scheme.
     */
    private val masterKey : MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Reads and decrypts the entire content of a file as a [ByteArray].
     *
     * @param fileName The name of the encrypted file to read. Must not contain directory separators.
     * @param directory Optional directory where the file is stored. Falls back to [defaultDirectory] if null.
     * @return [Result] wrapping the decrypted [ByteArray] on success, or a failure exception
     *         ([java.io.FileNotFoundException], [java.security.GeneralSecurityException], etc.).
     */

    suspend fun readBytes(filename: String, directory: File? = null): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = directory ?: defaultDirectory
            val file = File(targetDir, filename)

            if (!file.exists()) {
                throw NoSuchFileException(file, reason = "Encrypted file does not exist.")
            }

            createEncryptedFile(file).openFileInput().use { it.readBytes() }
        }
    }

    /**
     * Reads and decrypts the content of a file, decoding it into a [String].
     *
     * @param fileName The name of the encrypted file to read.
     * @param directory Optional directory where the file is stored. Falls back to [defaultDirectory] if null.
     * @param charset The character set used to decode the plaintext bytes. Defaults to [StandardCharsets.UTF_8].
     * @return [Result] containing the decrypted text content, or the associated error.
     */
    suspend fun readText(filename: String, directory: File? = null, charset: Charset = Charsets.UTF_8):
            Result<String> = readBytes(filename,directory).map {bytes ->
        String(bytes, charset)
    }

    /**
     * Encrypts and writes raw binary content to a file.
     *
     * If a file with the given name already exists in the destination directory, it will be deleted
     * prior to writing to ensure a clean ciphertext state.
     *
     * @param fileName The name of the file to create or overwrite.
     * @param content The raw byte payload to encrypt and store.
     * @param directory Optional target directory. Falls back to [defaultDirectory] if null.
     * @return [Result] indicating success ([Unit]) or wrapping any encountered I/O or crypto exception.
     */
    suspend fun writeBytes(filename: String, content: ByteArray, directory: File? = null) = withContext(Dispatchers.IO) {
        runCatching {
            val targetDir = directory ?: defaultDirectory
            val file = File(targetDir, filename)

            if(!targetDir.exists()){
                targetDir.mkdirs()
            }
            if (file.exists()) {
                file.delete()
            }
            createEncryptedFile(file).openFileOutput().use { output ->
                output.write(content)
                output.flush()
            }
        }
    }

    /**
     * Encrypts and writes a string payload to a file using the specified character encoding.
     *
     * @param fileName The name of the file to create or overwrite.
     * @param content The text payload to encrypt and persist.
     * @param directory Optional target directory. Falls back to [defaultDirectory] if null.
     * @param charset The character set used to encode the string into bytes. Defaults to [StandardCharsets.UTF_8].
     * @return [Result] indicating success ([Unit]) or wrapping any encountered error.
     */
    suspend fun writeText(filename: String, content: String, directory: File? = null, charset: Charset = Charsets.UTF_8):
            Result<Unit> = writeBytes( filename, content.toByteArray(charset), directory)

    /**
     * Deletes the specified encrypted file from storage.
     *
     * @param fileName The name of the file to remove.
     * @param directory Optional directory where the file is stored. Falls back to [defaultDirectory] if null.
     * @return `true` if the file existed and was successfully deleted; `false` otherwise.
     */
    suspend fun deleteFile(fileName: String, directory: File? = null) = withContext(Dispatchers.IO) {
        val targetDir = directory ?: defaultDirectory
        val file = File(targetDir, fileName)

        file.exists() && file.delete()
    }

    /**
     * Checks whether an encrypted file exists on disk.
     *
     * @param fileName The name of the file to check.
     * @param directory Optional directory to look into. Falls back to [defaultDirectory] if null.
     * @return `true` if the file exists; `false` otherwise.
     */
    fun fileExists( fileName:String, directory: File? = null): Boolean {
        val targetDir = directory ?: defaultDirectory
        return File(targetDir, fileName).exists()
    }

    /**
     * Builds an [EncryptedFile] instance bound to the provided physical file and the active [MasterKey].
     */
    private fun createEncryptedFile(file: File) : EncryptedFile{
        return EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
    }
}