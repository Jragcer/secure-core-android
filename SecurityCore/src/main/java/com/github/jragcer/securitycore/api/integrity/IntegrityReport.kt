package com.github.jragcer.securitycore.api.integrity

data class IntegrityReport (
    val isSecure: Boolean,
    val detectedThreats: MutableList<ThreatType>,
    val details: Map<String, Boolean> = emptyMap()
)