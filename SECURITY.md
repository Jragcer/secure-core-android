# Security Policy

## Supported Versions

We actively provide security patches and updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.7.x   | :white_check_mark: |
| 1.6.x   | :white_check_mark: |
| < 1.6.0 | :x:                |

Always use the latest released version of `SecureCore`.

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a potential vulnerability or security bypass in `SecureCore`, please report it privately using GitHub Private Vulnerability Reporting:

1. Go to the repository's **Security** tab.
2. Click **Report a vulnerability** to open a private advisory draft.

### What to Include in Your Report

To help triage and reproduce the issue quickly, please provide:

* **Module affected:** (e.g., `NetworkSecurityManager`, `SecurePreferences`, `BiometricAuthManager`, `CryptoManager`, etc.).
* **Target environment:** Android version, API level, device model, or emulator.
* **Vulnerability details:** Clear description of the vulnerability or bypass mechanism.
* **Proof of Concept (PoC):** Step-by-step instructions or sample code to reproduce the issue.
* **Potential impact:** An assessment of how this issue affects client applications.

### What to Expect

This project is maintained by a single developer. Reports are reviewed on a best-effort basis, with no guaranteed response or fix times. Please allow reasonable time for a fix before any public disclosure. Reporters will be credited in the release notes unless they prefer to remain anonymous.

### Scope

`SecureCore` provides heuristic, best-effort protections (root, emulator, hooking and tamper detection) that a determined attacker may bypass. Bypasses of these heuristics are known limitations rather than vulnerabilities, unless they reveal a flaw in the library's own logic. See the disclaimer in the [README](README.md).