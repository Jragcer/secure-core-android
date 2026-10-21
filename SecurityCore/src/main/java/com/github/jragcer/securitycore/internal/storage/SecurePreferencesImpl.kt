package com.github.jragcer.securitycore.internal.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import androidx.core.content.edit
import com.github.jragcer.securitycore.api.storage.SecurePreferences
import java.lang.reflect.Type

/**
 * Thread-safe wrapper around [EncryptedSharedPreferences] for encrypted key-value persistence.
 *
 * All keys are encrypted using deterministic AES-256 SIV ([EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV])
 * to support key lookups, while all values are encrypted using non-deterministic AES-256 GCM
 * ([EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM]).
 *
 * Keys and master encryption schemes are managed transparently by the Android Keystore via [MasterKey].
 * Additionally, JSON object serialization and deserialization are supported via [Gson].
 *
 * Instances must be created using the [SecurePreferencesImpl.create] factory method to catch
 * and gracefully handle potential Keystore initialization failures.
 *
 * @property prefs The underlying [SharedPreferences] implementation (backed by [EncryptedSharedPreferences]).
 * @property gson The serializer instance used for complex object storage.
 */
internal class SecurePreferencesImpl (
    private val prefs: SharedPreferences,
    private val gson: Gson = Gson()
) : SecurePreferences
{
    /**
     * Persists a string value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The text value to encrypt and store, or `null` to remove/clear the entry.
     */
    override fun putString(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    /**
     * Retrieves an encrypted string value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key does not exist. Defaults to `null`.
     * @return The decrypted string value, or [defaultValue] if not found.
     */
    override fun getString(key: String, defaultValue: String? ): String? {
        return prefs.getString(key, defaultValue)
    }

    /**
     * Persists a boolean flag associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The boolean flag to encrypt and store.
     */
    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    /**
     * Retrieves an encrypted boolean flag.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `false`.
     * @return The decrypted boolean value, or [defaultValue] if not found.
     */
    override fun getBoolean(key: String, defaultValue: Boolean ): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * Persists an integer value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The integer to encrypt and store.
     */
    override fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    /**
     * Retrieves an encrypted integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0`.
     * @return The decrypted integer value, or [defaultValue] if not found.
     */
    override fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * Persists a long integer associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The long integer to encrypt and store.
     */
    override fun putLong(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    /**
     * Retrieves an encrypted long integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0L`.
     * @return The decrypted long value, or [defaultValue] if not found.
     */
    override fun getLong(key: String, defaultValue: Long ): Long {
        return prefs.getLong(key, defaultValue)
    }

    /**
     * Persists a floating-point value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The float value to encrypt and store.
     */
    override fun putFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    /**
     * Retrieves an encrypted floating-point value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0.0f`.
     * @return The decrypted float value, or [defaultValue] if not found.
     */
    override fun getFloat(key: String, defaultValue: Float ): Float {
        return prefs.getFloat(key, defaultValue)
    }

    /**
     * Persists an arbitrary serializable object by converting it to JSON prior to encryption.
     *
     * @param key The preference key identifier.
     * @param value The object instance to serialize and encrypt, or `null` to clear the entry.
     */
    override fun <T> putObject(key: String, value: T?) {
        if (value == null) {
            remove(key)
        } else {
            val json = gson.toJson(value)
            putString(key, json)
        }
    }

    /**
     * Retrieves and deserializes a previously encrypted JSON object.
     *
     * @param key The preference key identifier.
     * @param type The type to deserialize the target JSON into.
     * @return The parsed object instance of type [T], or `null` if the key is missing or deserialization fails.
     */
    override fun <T> getObject(key: String, type: Type): T? {
        val json = getString(key, null) ?: return null
        return runCatching {
            gson.fromJson<T>(json, type)
        }.getOrNull()
    }

    /**
     * Checks whether an encrypted key exists in storage.
     *
     * @param key The preference key identifier to check.
     * @return `true` if the preference exists, `false` otherwise.
     */
    override fun contains(key: String): Boolean = prefs.contains(key)

    /**
     * Removes an entry from storage.
     *
     * @param key The preference key identifier to remove.
     */
    override fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    /**
     * Clears all encrypted key-value pairs stored in this preferences file.
     */
    override fun clear() {
        prefs.edit { clear() }
    }

    companion object {

        /**
         * Factory function that securely initializes [SecurePreferencesImpl].
         *
         * Generates or retrieves a hardware-backed [MasterKey] via Android Keystore and instantiates
         * the underlying [EncryptedSharedPreferences].
         *
         * @param context Application or activity context.
         * @param fileName File name where encrypted preferences are stored. Defaults to `"secure_preferences"`.
         * @param gson Custom [Gson] instance if specialized type adapters are required.
         * @return [Result] enclosing the configured [SecurePreferences] instance on success, or any
         *         [java.security.GeneralSecurityException] / [java.io.IOException] encountered during initialization.
         */
        fun create(
            context: Context,
            fileName: String = "secure_preferences",
            gson: Gson = Gson()
        ): Result<SecurePreferences> = runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefs = EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            SecurePreferencesImpl(sharedPrefs, gson)
        }
    }
}