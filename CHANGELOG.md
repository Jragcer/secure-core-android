# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### Added
- Future features in development.

---

## [1.0.0] - 2026-10-20

### Added
- **`PassphraseManager`**: High-entropy cryptographic key generation and persistence backed by Android Keystore and `EncryptedFile` (AES-256 GCM HKDF 4KB).
- Automated sanitization of `0x00` null bytes in passphrases for full SQLCipher compatibility.
- Safe recovery mechanism for corrupted or 0-byte key files.
- **`SecurePreferences`**: Type-safe encrypted key-value storage using `EncryptedSharedPreferences`.
- JSON object serialization and deserialization support with typed generics (`putObject` / `getObject`).
- Automated key removal when assigning `null` values.
- **`SecureFileStorage`**: Asynchronous, coroutine-based encrypted file I/O operations with UTF-8 encoding support and binary buffer handling.
- Consumer ProGuard/R8 rules (`consumer-rules.pro`) packaged into the library AAR.
- Instrumented test suite (`androidTest`) covering edge cases, persistence, and cryptographic recovery.
- Continuous Integration workflow with GitHub Actions (`build.yaml`) validating builds, unit tests, and linting.
- Bilingual documentation (English and Spanish) in root `README.md`.