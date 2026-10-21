package com.github.jragcer.securitycore.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.storage.SecureFileStorage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SecureFileStorageTest {

    private lateinit var storage: SecureFileStorage
    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setUp(){
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.filesDir, "test_secure_files").apply { mkdirs() }
        storage = SecureCore.createSecureFileStorage(context)
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun writeAndReadText_matchesOriginalContent() = runTest {
        val fileName = "encrypted_note.txt"
        val expectedText = "This is a secret note."

        val writeResult = storage.writeText(fileName, expectedText)
        assertTrue(writeResult.isSuccess)
        assertTrue(storage.fileExists(fileName))

        val readResult = storage.readText(fileName)
        assertTrue(readResult.isSuccess)
        assertEquals(expectedText, readResult.getOrNull())
    }

    @Test
    fun writeAndReadText_preservesUtf8Encoding() = runTest {
        val fileName = "utf8_document.txt"
        val textWithSpecialChars = "Contraseña: ñandú, café, €100, 🔒 emojis y acentos."

        val writeResult = storage.writeText(fileName, textWithSpecialChars)
        assertTrue("Error al escribir texto: ${writeResult.exceptionOrNull()?.message}", writeResult.isSuccess)

        val readResult = storage.readText(fileName)
        assertTrue("Error al leer texto: ${readResult.exceptionOrNull()?.message}", readResult.isSuccess)
        assertEquals(textWithSpecialChars, readResult.getOrNull())
    }

    @Test
    fun writeAndReadBytes_preservesExactBinaryBuffers() = runTest {
        val fileName = "raw_binary.bin"
        val binaryData = ByteArray(256) { index -> (index - 128).toByte() }

        val writeResult = storage.writeBytes(fileName, binaryData)
        assertTrue("Error al escribir bytes: ${writeResult.exceptionOrNull()?.message}", writeResult.isSuccess)

        val readResult = storage.readBytes(fileName)
        assertTrue("Error al leer bytes: ${readResult.exceptionOrNull()?.message}", readResult.isSuccess)

        val readData = readResult.getOrNull()!!
        assertTrue("El buffer binario leído debe ser exactamente igual al escrito", binaryData.contentEquals(readData))
    }

    @Test
    fun writeText_overwritesExistingFileSafely() = runTest {
        val fileName = "overwrite_test.txt"
        val initialContent = "Versión original 1.0"
        val updatedContent = "Versión modificada 2.0 con nuevo tamaño"

        storage.writeText(fileName, initialContent)
        val overwriteResult = storage.writeText(fileName, updatedContent)

        assertTrue("La sobreescritura debe ser exitosa", overwriteResult.isSuccess)
        val readResult = storage.readText(fileName)
        assertEquals(updatedContent, readResult.getOrNull())
    }

    @Test
    fun readText_returnsFailure_whenFileDoesNotExist() = runTest {
        val result = storage.readText("non_existent_file.txt")

        assertTrue("Debe retornar un Result.failure", result.isFailure)
        assertFalse(result.isSuccess)
    }

    @Test
    fun deleteFile_removesFileFromDisk() = runTest {
        val fileName = "empty_file.txt"
        storage.writeText(fileName, "")
        assertTrue(storage.fileExists(fileName))

        val deleteResult = storage.deleteFile(fileName)
        assertTrue(deleteResult)
        assertFalse(storage.fileExists(fileName))
    }

    @Test
    fun deleteFile_and_fileExists_manageLifecycle() = runTest {
        val fileName = "lifecycle_file.txt"
        assertFalse(storage.fileExists(fileName))

        storage.writeText(fileName, "temporal")
        assertTrue(storage.fileExists(fileName))

        val deleted = storage.deleteFile(fileName)
        assertTrue("El borrado debe retornar true", deleted)
        assertFalse("El fichero ya no debe existir", storage.fileExists(fileName))

        // Intentar borrar un fichero que ya no existe debe devolver false
        val secondDelete = storage.deleteFile(fileName)
        assertFalse(secondDelete)
    }
}