# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]
### Added
- Future features in development.

---

## [1.1.0] - 2026-10-21

### Added
- **Biometric Authentication Module (`api/BiometricAuthManager`)**:
    - Hardware and enrollment status verification via `canAuthenticate()`.
    - Native biometric dialog orchestration through `authenticate()`.
    - Cryptographic-bound authentication support (`authenticateWithCrypto()`) with Android Keystore.
    - Type-safe outcome contracts: `BiometricResult` (Success, Error, Failed) and `BiometricStatus`.
    - Public factory access via `SecureCore.createBiometricAuthManager(context)`.
- **Demo App Updates**:
    - Implemented an interactive demo view (`MainActivity`) combining tests for existing storage primitives (`PassphraseManagerImpl`, `SecurePreferencesImpl`, `SecureFileStorageImpl`) alongside the new biometric authentication module.
    - Added real-time visual output and status logs for all security operations.

### Changed
- **Architectural Unification (API / Internal / Factory)**:
  - Extracted public interface contracts for the storage suite under `api/storage/`: `PassphraseManager`, `SecurePreferences`, and `SecureFileStorage`.
  - Encapsulated concrete storage engines as package-private implementations under `internal/storage/`: `PassphraseManagerImpl`, `SecurePreferencesImpl`, and `SecureFileStorageImpl`.
  - Unified SDK entry points under the `SecureCore` facade with dedicated factory methods: `createPassphraseManager()`, `createSecurePreferences()`, and `createSecureFileStorage()`.
  - Provided public inline reified extensions for `SecurePreferences` (`getObject<T>()` and `putObject<T>()`) to maintain convenient JSON model serialization via Gson without leaking internal implementation details.

---

## [1.0.0] - 2026-10-20

### Added
- **`PassphraseManagerImpl`**: High-entropy cryptographic key generation and persistence backed by Android Keystore and `EncryptedFile` (AES-256 GCM HKDF 4KB).
- Automated sanitization of `0x00` null bytes in passphrases for full SQLCipher compatibility.
- Safe recovery mechanism for corrupted or 0-byte key files.
- **`SecurePreferencesImpl`**: Type-safe encrypted key-value storage using `EncryptedSharedPreferences`.
- JSON object serialization and deserialization support with typed generics (`putObject` / `getObject`).
- Automated key removal when assigning `null` values.
- **`SecureFileStorageImpl`**: Asynchronous, coroutine-based encrypted file I/O operations with UTF-8 encoding support and binary buffer handling.
- Consumer ProGuard/R8 rules (`consumer-rules.pro`) packaged into the library AAR.
- Instrumented test suite (`androidTest`) covering edge cases, persistence, and cryptographic recovery.
- Continuous Integration workflow with GitHub Actions (`build.yaml`) validating builds, unit tests, and linting.
- Bilingual documentation (English and Spanish) in root `README.md`.