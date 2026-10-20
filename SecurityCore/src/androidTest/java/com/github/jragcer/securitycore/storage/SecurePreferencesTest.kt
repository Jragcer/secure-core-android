package com.github.jragcer.securitycore.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecurePreferencesTest {
    private lateinit var securePrefs: SecurePreferences
    private lateinit var context: Context

    data class UserSession(val userId: String, val role: String, val active: Boolean)

    @Before
    fun setUp(){
        context = ApplicationProvider.getApplicationContext()
        val result = SecurePreferences.create(context, "test_prefs")

        // Si falla, imprime la traza real para ver la causa raíz:
        result.onFailure { error ->
            error.printStackTrace()
        }

        assertTrue(result.isSuccess)
        securePrefs = result.getOrNull()!!
        securePrefs.clear()
    }

    @After
    fun tearDown() {
        securePrefs.clear()
    }

    @Test
    fun putAndGetString_worksCorrectly(){
        securePrefs.putString("auth_token", "secret_jwt_token")

        val retrieved = securePrefs.getString("auth_token")
        assertEquals("secret_jwt_token", retrieved)
    }

    @Test
    fun putAndGetBoolean_worksCorrectly(){
        securePrefs.putBoolean("is_biometric_enabled", true)

        val retrieved = securePrefs.getBoolean("is_biometric_enabled", defaultValue = false)
        assertTrue(retrieved)
    }

    @Test
    fun putAndGetInt_worksCorrectly() {
        securePrefs.putInt("user_id", 12345)

        val retrieved = securePrefs.getInt("user_id")
        assertEquals(12345, retrieved)
    }

    @Test
    fun putAndGetLong_worksCorrectly() {
        securePrefs.putLong("user_id", 1234567890L)

        val retrieved = securePrefs.getLong("user_id")
        assertEquals(1234567890L, retrieved)
    }

    @Test
    fun putAndGetFloat_worksCorrectly() {
        securePrefs.putFloat("user_id", 12345.34f)

        val retrieved = securePrefs.getFloat("user_id")
        assertEquals(12345.34f, retrieved)
    }

    @Test
    fun putAndGetObject_worksCorrectly() {
        val session = UserSession("user_99", "admin", true)
        securePrefs.putObject("session", session)

        val retrieved = securePrefs.getObject<UserSession>("session")
        assertNotNull(retrieved)
        assertEquals("user_99", retrieved!!.userId)
        assertEquals("admin", retrieved.role)
        assertTrue(retrieved.active)
    }

    @Test
    fun get_returnsDefaultValue_whenKeyDoesNotExist() {
        assertNull(securePrefs.getString("non_existent_str"))
        assertEquals("default_str", securePrefs.getString("non_existent_str", "default_str"))
        assertFalse(securePrefs.getBoolean("non_existent_bool", false))
        assertEquals(-1, securePrefs.getInt("non_existent_int", -1))
        assertEquals(-1L, securePrefs.getLong("non_existent_long", -1L))
        assertEquals(0.5f, securePrefs.getFloat("non_existent_float", 0.5f))
        assertNull(securePrefs.getObject<UserSession>("non_existent_obj"))
    }

    @Test
    fun putObject_withNull_removesKey() {
        val profile = UserSession( "usr_10", "admin", true)
        securePrefs.putObject("profile_key", profile)
        assertTrue(securePrefs.contains("profile_key"))

        securePrefs.putObject<UserSession>("profile_key", null)
        assertFalse(securePrefs.contains("profile_key"))
        assertNull(securePrefs.getObject<UserSession>("profile_key"))
    }

    @Test
    fun remove_deletesKeySuccessfully(){
        securePrefs.putString("temp_data", "to_be_deleted")
        assertTrue(securePrefs.contains("temp_data"))

        securePrefs.remove("temp_data")
        assertFalse(securePrefs.contains("temp_data"))
        assertNull(securePrefs.getString("temp_data"))
    }

    @Test
    fun clear_removeAllKeysSuccessfully(){
        securePrefs.putString("key1", "val1")
        securePrefs.putString("key2", "val2")

        assertTrue(securePrefs.contains("key1"))
        assertTrue(securePrefs.contains("key2"))

        // Borrado individual
        securePrefs.remove("key1")
        assertFalse(securePrefs.contains("key1"))
        assertTrue(securePrefs.contains("key2"))

        // Borrado completo
        securePrefs.clear()
        assertFalse(securePrefs.contains("key2"))
        assertNull(securePrefs.getString("key2"))
    }
}