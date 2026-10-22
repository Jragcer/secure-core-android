package com.github.jragcer.securitycore.api.integrity

enum class ThreatType {
    ROOT_DETECTED,
    EMULATOR_DETECTED,
    DEBUGGER_ATTACHED,
    HOOKING_FRAMEWORK_DETECTED,
    TEST_KEYS_BUILD
}