package com.github.jragcer.securitycore.api.storage

import android.content.Context
import java.io.File
import java.nio.charset.Charset

/**
 *  Public interface for encrypted file I/O operations.
 */
interface SecureFileStorage {
    /**
     * Encrypts and writes raw binary content to a file.
     *
     * If a file with the given name already exists in the destination directory, it will be deleted
     * prior to writing to ensure a clean ciphertext state.
     *
     * @param fileName The name of the file to create or overwrite.
     * @param content The raw byte payload to encrypt and store.
     * @param directory Optional target directory. Falls back to [Context.getFilesDir] if null.
     * @return [Result] indicating success ([Unit]) or wrapping any encountered I/O or crypto exception.
     */
    suspend fun writeBytes(fileName: String, content: ByteArray, directory: File? = null): Result<Unit>
    /**
     * Reads and decrypts the entire content of a file as a [ByteArray].
     *
     * @param fileName The name of the encrypted file to read. Must not contain directory separators.
     * @param directory Optional directory where the file is stored. Falls back to [Context.getFilesDir] if null.
     * @return [Result] wrapping the decrypted [ByteArray] on success, or a failure exception
     *         ([java.io.FileNotFoundException], [java.security.GeneralSecurityException], etc.).
     */
    suspend fun readBytes(fileName: String, directory: File? = null): Result<ByteArray>
    /**
     * Encrypts and writes a string payload to a file using the specified character encoding.
     *
     * @param fileName The name of the file to create or overwrite.
     * @param content The text payload to encrypt and persist.
     * @param directory Optional target directory. Falls back to [Context.getFilesDir] if null.
     * @param charset The character set used to encode the string into bytes. Defaults to [Charsets.UTF_8].
     * @return [Result] indicating success ([Unit]) or wrapping any encountered error.
     */
    suspend fun writeText(fileName: String, content: String, directory: File? = null, charset: Charset = Charsets.UTF_8): Result<Unit>
    /**
     * Reads and decrypts the content of a file, decoding it into a [String].
     *
     * @param fileName The name of the encrypted file to read.
     * @param directory Optional directory where the file is stored. Falls back to [Context.getFilesDir] if null.
     * @param charset The character set used to decode the plaintext bytes. Defaults to [Charsets.UTF_8].
     * @return [Result] containing the decrypted text content, or the associated error.
     */
    suspend fun readText(fileName: String, directory: File? = null, charset: Charset = Charsets.UTF_8): Result<String>

    /**
     * Checks whether an encrypted file exists on disk.
     *
     * @param fileName The name of the file to check.
     * @param directory Optional directory to look into. Falls back to [Context.getFilesDir] if null.
     * @return `true` if the file exists; `false` otherwise.
     */
    fun fileExists(fileName: String, directory: File? = null): Boolean
    /**
     * Deletes the specified encrypted file from storage.
     *
     * @param fileName The name of the file to remove.
     * @param directory Optional directory where the file is stored. Falls back to [Context.getFilesDir] if null.
     * @return `true` if the file existed and was successfully deleted; `false` otherwise.
     */
    suspend fun deleteFile(fileName: String, directory: File? = null): Boolean

}