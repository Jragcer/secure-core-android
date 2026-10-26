package com.github.jragcer.securitycore.internal.network

import android.content.Context
import com.github.jragcer.securitycore.api.network.NetworkSecurityManager
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import java.security.KeyStore

internal class NetworkSecurityManagerImpl (
    private val context: Context
): NetworkSecurityManager {
    /**
     * Builds an OkHttp [CertificatePinner] instance configured with the specified host-to-pin mappings.
     * Pins must be provided in SHA-256 format (e.g., "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").
     *
     * @param pins Map of domain names to their expected SHA-256 certificate hashes.
     * @return Configured [CertificatePinner].
     */
    override fun createCertificatePinner(pins: Map<String, List<String>>): CertificatePinner{
        val builder = CertificatePinner.Builder()
        for ((pattern, hashes) in pins) {
            for (hash in hashes) {
                val formattedPin = if (hash.startsWith("sha256/")) hash else "sha256/$hash"
                builder.add(pattern, formattedPin)
            }
        }
        return builder.build()
    }

    /**
     * Applies certificate pinning rules directly to an [OkHttpClient.Builder].
     *
     * @param builder The OkHttp client builder to configure.
     * @param pins Map of domain names to expected SHA-256 public key pins.
     * @return The updated [OkHttpClient.Builder].
     */
    override fun applyPinning(builder: OkHttpClient.Builder, pins: Map<String, List<String>>): OkHttpClient.Builder{
        val pinner = createCertificatePinner(pins)
        return builder.certificatePinner(pinner)
    }

    /**
     * Inspects the device trust store to verify whether user-installed or untrusted Certificate Authorities (CAs)
     * exist in the system, which could indicate an active MitM proxy (e.g., Charles, Burp Suite, mitmproxy).
     *
     * @return True if suspicious or user-installed CAs are present in the trust store.
     */
    override fun hasUserInstalledCertificates(): Boolean{
        return try {
            val keyStore = KeyStore.getInstance("AndroidCAStore").apply {
                load(null, null)
            }
            val aliases = keyStore.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                // In AndroidCAStore, certificates installed by the user or via MDM profiles
                // use the prefix "user:"
                if (alias.startsWith("user:")) {
                    return true
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }
}