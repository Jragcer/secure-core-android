# Security Policy

## Supported Versions

We actively provide security patches and updates for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.5.x   | :white_check_mark: |
| 1.4.x   | :white_check_mark: |
| < 1.4.0 | :x:                |

We strongly recommend always using the latest released version of `SecurityCore` to ensure all security patches, API 34+ compatibility improvements, and cryptographic safeguards are active.

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a potential vulnerability or security bypass in `SecurityCore`, please disclose it privately using one of the following methods:

1. **GitHub Private Security Advisory (Preferred):**
    * Go to the repository's **Security** tab.
    * Click on **Report a vulnerability** to open a private advisory draft.

2. **Email Disclosure:** (In progress)
    * Send an email to: `tu-email-de-contacto@dominio.com`
    * Subject line: `[SecurityCore] Vulnerability Report: <Brief summary>`

### What to Include in Your Report

To help us triage and reproduce the issue quickly, please provide:
* **Module affected:** (e.g., `NetworkSecurityManager`, `SecurePreferences`, `BiometricAuthManager`, etc.).
* **Target environment:** Android version, API level, device model, or emulator.
* **Vulnerability details:** Clear description of the vulnerability or security bypass mechanism.
* **Proof of Concept (PoC):** Step-by-step instructions or sample code to reproduce the issue.
* **Potential impact:** An assessment of how this issue affects client applications.