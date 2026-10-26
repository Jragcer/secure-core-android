package com.github.jragcer.securitycore.api.network

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Public interface providing network security mechanisms such as SSL/TLS Certificate Pinning
 * and detection of untrusted/user-installed Certificate Authorities (CAs).
 */
interface NetworkSecurityManager {
    /**
     * Builds an OkHttp [CertificatePinner] instance configured with the specified host-to-pin mappings.
     * Pins must be provided in SHA-256 format (e.g., "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=").
     *
     * @param pins Map of domain names to their expected SHA-256 certificate hashes.
     * @return Configured [CertificatePinner].
     */
    fun createCertificatePinner(pins: Map<String, List<String>>): CertificatePinner

    /**
     * Applies certificate pinning rules directly to an [OkHttpClient.Builder].
     *
     * @param builder The OkHttp client builder to configure.
     * @param pins Map of domain names to expected SHA-256 public key pins.
     * @return The updated [OkHttpClient.Builder].
     */
    fun applyPinning(builder: OkHttpClient.Builder, pins: Map<String, List<String>>): OkHttpClient.Builder

    /**
     * Inspects the device trust store to verify whether user-installed or untrusted Certificate Authorities (CAs)
     * exist in the system, which could indicate an active MitM proxy (e.g., Charles, Burp Suite, mitmproxy).
     *
     * @return True if suspicious or user-installed CAs are present in the trust store.
     */
    fun hasUserInstalledCertificates(): Boolean
}