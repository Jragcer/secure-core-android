package com.github.jragcer.securitycore.api.storage

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 *  Public interface for encrypted key-value persistence.
 */
interface SecurePreferences {

    /**
     * Persists a string value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The text value to encrypt and store, or `null` to remove/clear the entry.
     */
    fun putString(key: String, value: String?)
    /**
     * Retrieves an encrypted string value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key does not exist. Defaults to `null`.
     * @return The decrypted string value, or [defaultValue] if not found.
     */
    fun getString(key: String, defaultValue: String? = null): String?

    /**
     * Persists a boolean flag associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The boolean flag to encrypt and store.
     */
    fun putBoolean(key: String, value: Boolean)
    /**
     * Retrieves an encrypted boolean flag.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `false`.
     * @return The decrypted boolean value, or [defaultValue] if not found.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * Persists an integer value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The integer to encrypt and store.
     */
    fun putInt(key: String, value: Int)
    /**
     * Retrieves an encrypted integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0`.
     * @return The decrypted integer value, or [defaultValue] if not found.
     */
    fun getInt(key: String, defaultValue: Int = 0): Int

    /**
     * Persists a long integer associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The long integer to encrypt and store.
     */
    fun putLong(key: String, value: Long)
    /**
     * Retrieves an encrypted long integer value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0L`.
     * @return The decrypted long value, or [defaultValue] if not found.
     */
    fun getLong(key: String, defaultValue: Long = 0L): Long

    /**
     * Persists a floating-point value associated with the specified key.
     *
     * @param key The preference key identifier.
     * @param value The float value to encrypt and store.
     */
    fun putFloat(key: String, value: Float)
    /**
     * Retrieves an encrypted floating-point value.
     *
     * @param key The preference key identifier.
     * @param defaultValue The fallback value if the key is not present. Defaults to `0.0f`.
     * @return The decrypted float value, or [defaultValue] if not found.
     */
    fun getFloat(key: String, defaultValue: Float = 0f): Float

    /**
     * Persists an arbitrary serializable object by converting it to JSON prior to encryption.
     *
     * @param key The preference key identifier.
     * @param value The object instance to serialize and encrypt, or `null` to clear the entry.
     */
    fun <T> putObject(key: String, value: T?)
    /**
     * Retrieves and deserializes a previously encrypted JSON object.
     *
     * @param key The preference key identifier.
     * @param type The type to deserialize the target JSON into.
     * @return The parsed object instance of type [T], or `null` if the key is missing or deserialization fails.
     */
    fun <T> getObject(key: String, type: Type): T?

    /**
     * Checks whether an encrypted key exists in storage.
     *
     * @param key The preference key identifier to check.
     * @return `true` if the preference exists, `false` otherwise.
     */
    fun contains(key: String): Boolean
    /**
     * Removes an entry from storage.
     *
     * @param key The preference key identifier to remove.
     */
    fun remove(key: String)
    /**
     * Clears all encrypted key-value pairs stored in this preferences file.
     */
    fun clear()
}

/**
 * Inline reified extension for convenient model retrieval without passing Type.
 */
inline fun <reified T> SecurePreferences.getObject(key: String): T? {
    val type = object : TypeToken<T>() {}.type
    return getObject(key, type)
}

inline fun <reified T> SecurePreferences.putObject(key: String, value: T?) {
    val type = object : TypeToken<T>() {}.type
    if (value == null) {
        remove(key)
    } else {
        putObject(key, value)
    }
}