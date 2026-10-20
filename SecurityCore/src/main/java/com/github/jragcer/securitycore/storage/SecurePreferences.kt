package com.github.jragcer.securitycore.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import androidx.core.content.edit

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
 * Instances must be created using the [SecurePreferences.create] factory method to catch
 * and gracefully handle potential Keystore initialization failures.
 *
 * @property prefs The underlying [SharedPreferences] implementation (backed by [EncryptedSharedPreferences]).
 * @property gson The serializer instance used for complex object storage.
 */
class SecurePreferences private constructor(@PublishedApi internal val prefs: SharedPreferences,
                                            @PublishedApi internal val gson: Gson = Gson()
)
{
    /**
     * Persists a string value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The text value to encrypt and store, or `null` to remove/clear the entry.
     */
    fun putString(key: String, value: String?) {
        prefs.edit { putString(key, value) }
    }

    /**
     * Retrieves an encrypted string value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key does not exist. Defaults to `null`.
     * @return The decrypted string value, or [defaultValue] if not found.
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }

    /**
     * Persists a boolean flag associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The boolean flag to encrypt and store.
     */
    fun putBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    /**
     * Retrieves an encrypted boolean flag.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `false`.
     * @return The decrypted boolean value, or [defaultValue] if not found.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    /**
     * Persists an integer value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The integer to encrypt and store.
     */
    fun putInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    /**
     * Retrieves an encrypted integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0`.
     * @return The decrypted integer value, or [defaultValue] if not found.
     */
    fun getInt(key: String, defaultValue: Int = 0): Int {
        return prefs.getInt(key, defaultValue)
    }

    /**
     * Persists a long integer associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The long integer to encrypt and store.
     */
    fun putLong(key: String, value: Long) {
        prefs.edit { putLong(key, value) }
    }

    /**
     * Retrieves an encrypted long integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0L`.
     * @return The decrypted long value, or [defaultValue] if not found.
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return prefs.getLong(key, defaultValue)
    }

    /**
     * Persists a floating-point value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The float value to encrypt and store.
     */
    fun putFloat(key: String, value: Float) {
        prefs.edit { putFloat(key, value) }
    }

    /**
     * Retrieves an encrypted floating-point value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0.0f`.
     * @return The decrypted float value, or [defaultValue] if not found.
     */
    fun getFloat(key: String, defaultValue: Float = 0.0f): Float {
        return prefs.getFloat(key, defaultValue)
    }

    /**
     * Persists an arbitrary serializable object by converting it to JSON prior to encryption.
     *
     * @param T The expected data type of the object.
     * @param key The preference key identifier.
     * @param value The object instance to serialize and encrypt, or `null` to clear the entry.
     */
    inline fun <reified T> putObject(key: String, value: T?) {
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
     * @param T The type to deserialize the target JSON into.
     * @param key The preference key identifier.
     * @return The parsed object instance of type [T], or `null` if the key is missing or deserialization fails.
     */
    inline fun <reified T> getObject(key: String): T? {
        val json = getString(key, null) ?: return null
        return runCatching {
            gson.fromJson(json, T::class.java)
        }.getOrNull()
    }

    /**
     * Checks whether an encrypted key exists in storage.
     *
     * @param key The preference key identifier to check.
     * @return `true` if the preference exists, `false` otherwise.
     */
    fun contains(key: String): Boolean = prefs.contains(key)

    /**
     * Removes an entry from storage.
     *
     * @param key The preference key identifier to remove.
     */
    fun remove(key: String) {
        prefs.edit { remove(key) }
    }

    /**
     * Clears all encrypted key-value pairs stored in this preferences file.
     */
    fun clear() {
        prefs.edit { clear() }
    }

    /**
     * Exposes the underlying [SharedPreferences] instance if raw access is required.
     */
    fun getRawSharedPreferences(): SharedPreferences = prefs

    companion object {

        /**
         * Factory function that securely initializes [SecurePreferences].
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
            SecurePreferences(sharedPrefs, gson)
        }
    }
}