package com.github.jragcer.securitycore.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PassphraseManagerTest {
    private lateinit var manager: PassphraseManager
    private lateinit var context: Context
    private lateinit var testDir: File

    @Before
    fun setUp(){
        context = ApplicationProvider.getApplicationContext()
        testDir = File(context.filesDir, "test_keys").apply { mkdirs() }
        manager = PassphraseManager(context, testDir)
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun getOrCreatePassphrase_generatesValidKey_withExactLenght(){
        val sizes = listOf(16,32,64)

        for (size in sizes) {
            val fileName = "key_size_$size.bin"
            val result = manager.getOrCreatePassphrase(fileName, size)

            assertTrue("Fallo al generar clave de tamaño $size",result.isSuccess)
            val key = result.getOrNull()
            assertNotNull(key)
            assertEquals("El tamaño de la clave no coincide",size, key!!.size)
        }
    }

    @Test
    fun getOrCreatePassphrase_doesNotContainNullBytes(){
        val result = manager.getOrCreatePassphrase("sqlcipher_key.bin", 64)

        assertTrue(result.isSuccess)
        val key = result.getOrNull()!!
        val hasZeroBytes = key.any { it == 0.toByte() }
        assertFalse("La clave contiene bytes nulos", hasZeroBytes)
    }

    @Test
    fun getOrCreatePassphrase_returnsPersistedKey_onSubsequentCalls(){
        val fileName = "persistent_key.bin"
        val firstCall = manager.getOrCreatePassphrase(fileName, 32)
        val secondCall = manager.getOrCreatePassphrase(fileName, 32)

        assertTrue(firstCall.isSuccess)
        assertTrue(secondCall.isSuccess)

        val firstBytes = firstCall.getOrNull()
        val secondBytes = secondCall.getOrNull()
        assertTrue("Las claves persistidas deben ser idénticas", firstBytes.contentEquals( secondBytes))
    }

    @Test
    fun getOrCreatePassphrase_recoversFromEmptyOrCorruptedFile(){
        val fileName = "empty_file.bin"
        val corruptFile = File(testDir, fileName)

        // Crear fichero vacío (0 bytes) simulando interrupción previa
        FileOutputStream(corruptFile).use { /* No escribe nada */ }
        assertEquals(0L, corruptFile.length())

        val result = manager.getOrCreatePassphrase(fileName, 32)

        assertTrue("Debe recuperarse y generar una clave válida", result.isSuccess)
        val key = result.getOrNull()
        assertNotNull(key)
        assertEquals(32, key!!.size)
        assertTrue(corruptFile.length() > 0L)
    }

    @Test
    fun getOrCreatePassphrase_isolatesKeysAcrossDiferentFiles(){
        val firstCall = manager.getOrCreatePassphrase("key_a.bin", 32)
        val secondCall = manager.getOrCreatePassphrase("key_b.bin", 32)

        assertTrue(firstCall.isSuccess)
        assertTrue(secondCall.isSuccess)

        val firstBytes = firstCall.getOrNull()
        val secondBytes = secondCall.getOrNull()
        assertFalse("Dos ficheros distintos deben almacenar claves diferentes", firstBytes.contentEquals( secondBytes))
    }
}