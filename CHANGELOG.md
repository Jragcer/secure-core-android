# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.7.0] - 2026-10-27

### Added
- **Asymmetric Cryptography & Digital Signatures (`AsymmetricCryptoManager`):**
  - RSA and Elliptic Curve (EC P-256 / ECDSA) key pair generation in the `AndroidKeyStore`. The private key never leaves the Keystore; it is hardware-backed (TEE) only on devices that support it. StrongBox is not requested.EC keys use explicit P-256 curve configuration and SHA-256 digest.
  - RSA public-key encryption and Keystore-held private-key decryption with `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` and explicit `MGF1ParameterSpec.SHA1` (payload limit ~190 bytes for RSA-2048; use hybrid encryption for larger payloads).
  - Digital signature generation and verification (`SHA256withRSA` and `SHA256withECDSA`).
  - Standard X.509/DER public key export via `getPublicKeyEncoded()` for backend transmission.
  - Public factory method `SecureCore.createAsymmetricCryptoManager(alias, algorithm)`.
  - **Reliability & Boundary Validations:**
    - Serialized key pair generation preventing concurrent threads sharing an alias from producing conflicting keys.
    - Strict alias checks throwing a descriptive `IllegalStateException` on key type or size collisions.
    - Pre-flight payload size checks in `encrypt()` throwing `IllegalArgumentException` on oversized inputs.
    - Graceful signature verification returning `false` rather than throwing on malformed signature data.
- **README disclaimer:** AI-assisted development, no independent security audit, heuristic detection limits, and no warranty.
- **Test Coverage:**
  - Instrumented test suite (`androidTest`) covering the Keystore key lifecycle and asymmetric crypto operations.
  - JVM unit test suite (`test`) covering algorithm configuration models, X.509 reconstruction, and edge-case invariants.
- **Quality & Security Pipeline:**
  - **CodeQL** static analysis in GitHub Actions for automated vulnerability and CWE scanning.
  - **Codecov** integration for test coverage reporting across JVM and instrumented test suites.

### Changed
- `SecureCore.createCryptoManager(keyAlias, provider)` is deprecated: `provider` was never applied and the AndroidKeyStore is always used. Use `createCryptoManager(keyAlias)`.
- Documentation no longer claims StrongBox backing or absolute hardware protection, and the wording on detection, tamper checks, screen protection and certificate pinning now matches actual behaviour.
- Quick Start examples updated to the current API.

### Fixed
- `AsymmetricCryptoManager.verify()` returns `false` instead of throwing for malformed signatures.
- Creating a manager on an alias that already holds a different key type or size now fails immediately with a descriptive `IllegalStateException`.
- Key pair creation is now serialised, so concurrent instances sharing an alias cannot generate different keys.
- `AsymmetricCryptoManager.encrypt()` validates the RSA-OAEP payload limit and reports it with a descriptive `IllegalArgumentException`.
- EC keys now use an explicit P-256 curve and are limited to SHA-256.

---

## [1.6.0] - 2026-10-27

### Added
- **CryptoManager module**: Hardware-backed AES-256-GCM encryption with AndroidKeyStore.
  - Arbitrary `ByteArray` encryption and decryption with automatic IV and GCM authentication tag handling.
  - Streaming support via `InputStream` and `OutputStream` to process large payloads/files without high memory usage.
  - Domain-specific exception hierarchy (`CryptoException`).
  - Factory entrypoint in `SecureCore.createCryptoManager()`.
- Comprehensive Robolectric and hardware instrumented test coverage for key persistence, tamper resistance, and concurrency.

---

## [1.5.0] - 2026-10-26

### Added

- **Network Security & Certificate Pinning Module (`api/network`)**:
  - `NetworkSecurityManager`: Public contract interface providing network security capabilities to mitigate Man-in-the-Middle (MitM) attacks, enforce SSL/TLS certificate public key pinning, and audit device trust store certificates.
  - `NetworkSecurityManagerImpl`: Concrete implementation providing:
    - `createCertificatePinner()`: Utility configuring OkHttp `CertificatePinner` with SHA-256 public key hash rules (`sha256/...`).
    - `applyPinning()`: Seamless chaining for `OkHttpClient.Builder` to bind security pins directly to HTTP client instances.
    - `hasUserInstalledCertificates()`: Trust store heuristic auditing the system's `AndroidCAStore` for user-installed or MDM root certificates (prefixed with `user:`), detecting active network interception proxies (e.g., Charles Proxy, Burp Suite, mitmproxy).
  - Unified facade entrypoint via `SecureCore.createNetworkSecurityManager(context)`.

- **UI & Interactive Demo Integration**:
  - Integrated dedicated `Network Security & SSL Pinning` controls within `SecurityDemoScreen`.
  - Real-time MitM detection testing, invalid pin handshake rejection verification (`SSLPeerUnverifiedException`), and dynamic host public key pin discovery.

- **Dependencies & Build System**:
  - Integrated `com.squareup.okhttp3:okhttp` dependency across library and demo applications via `libs.versions.toml`.

---

## [1.4.0] - 2026-10-23

### Added
- **Screen & Window Privacy Protection Module (`api/screen`)**:
  - `ScreenProtectionManager`: Public contract interface providing runtime control over window security policies to prevent unauthorized screen captures, screen recording, and content exposure in the Android App Switcher (Recents screen).
  - `ScreenProtectionManagerImpl`: Encapsulated implementation dynamically applying and clearing `WindowManager.LayoutParams.FLAG_SECURE` on target `Activity` windows.
  - `SecureScreenEffect`: Lifecycle-aware Jetpack Compose composable helper (`DisposableEffect`) that automatically enforces screen protection while a composable or dialog is active, seamlessly restoring previous window states on disposal via optional state callbacks.
  - Unified facade initialization via `SecureCore.createScreenProtectionManager()`.
- **UI Components & Demo**:
  - Added `SensitiveDataModal` demonstrating automated Compose-level screen protection and state restoration.
  - UI integration inside `SecurityDemoScreen`.
- **Testing & Tooling**:
  - JVM unit test coverage (`ScreenProtectionManagerUnitTest`) verifying `FLAG_SECURE` flag operations with Mockito.
  - Instrumented test suite (`ScreenProtectionInstrumentedTest`) validating window security flags and Compose rule lifecycle across runtime Android devices.
  - Added test manifest configuration in `src/debug/AndroidManifest.xml` with `androidx.compose.ui:ui-test-manifest` integration.

---

## [1.3.0] - 2026-10-23

### Added
- **APK Tamper & Signature Protection Module (`api/tamper`)**:
  - `TamperProtectionManager`: Public interface for runtime signature hash validation and installer source auditing.
  - `TamperProtectionManagerImpl`: Robust implementation resolving X.509 certificates across Android P+ (`SigningInfo`) and legacy releases (`GET_SIGNATURES`), computing normalized SHA-256 fingerprints.
  - Installer verification supporting Android 11+ (`getInstallSourceInfo`) and legacy package managers to distinguish official stores (Google Play) from unauthorized sideloading.
  - `AppSignatureStatus`: Consolidated diagnostic model reporting signature matches, installer trust, active hash, and overall genuine status (`isGenuine`).
  - Factory entrypoint in `SecureCore.createTamperProtectionManager(context)`.
  - Comprehensive unit and instrumented test suites.
- **UI & Presentation**:
  - `ExpandableSectionCard`: Modular collapsible container with animated expand/collapse states for SDK feature blocks.
  - Interactive signature validation and certificate hash inspection controls in `SecurityDemoScreen`.
  
---

## [1.2.0] - 2026-10-22

### Added
- **Integrity & Threat Detection Module**:
  - `RootDetector`: Heuristic detection of `su` binaries, test-keys, dangerous mount flags, and root management packages.
  - `EmulatorDetector`: Virtual environment identification via QEMU drivers, pipes, and build hardware properties.
  - `HookDetector`: Memory map scanning (`/proc/self/maps`), debugger attachment flags, and Frida/Xposed signature verification.
  - `IntegrityManager` & `IntegrityManagerImpl`: Composite threat orchestration producing structured `IntegrityReport` summaries.
  - Factory entrypoint in `SecureCore.createIntegrityManager(context)`.
  - Comprehensive unit and instrumented test suites.
  - UI integration inside `SecurityDemoScreen`.

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