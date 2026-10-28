# SecureCore Android Library

[![Android CI](https://github.com/Jragcer/secure-core-android/actions/workflows/build.yaml/badge.svg)](https://github.com/Jragcer/secure-core-android/actions/workflows/build.yaml)
[![CodeQL](https://github.com/jragcer/secure-core-android/actions/workflows/codeql.yml/badge.svg)](https://github.com/jragcer/secure-core-android/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/jragcer/secure-core-android/branch/main/graph/badge.svg)](https://codecov.io/gh/jragcer/secure-core-android)
[![Latest Release](https://img.shields.io/badge/Release-1.7.0-blue.svg)](https://github.com/jragcer/secure-core-android/releases)
[![JitPack](https://jitpack.io/v/jragcer/secure-core-android.svg)](https://jitpack.io/#jragcer/secure-core-android)
[![License](https://img.shields.io/badge/Licencia-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-brightgreen.svg)](https://developer.android.com/tools/releases/platforms)
[![Compile SDK](https://img.shields.io/badge/Compile%20SDK-37-brightgreen.svg)](https://developer.android.com/tools/releases/platforms)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org/docs/whatsnew20.html)

*Read this in [English](#english) | Léelo en [Español](#español)*

---

<a name="english"></a>
## English

`SecureCore` is a lightweight Android security SDK designed to simplify Keystore-backed cryptography (AES-256-GCM, plus RSA/EC key pairs and digital signatures), secure local storage, biometric authentication, heuristic runtime integrity assessment, APK tamper detection, screen capture blocking, SSL/TLS certificate pinning, and detection of user-installed CAs (a common sign of interception proxies), with minimal boilerplate. Key material is hardware-backed only on devices that support it.
> **Current Version:** `1.7.0` (See [CHANGELOG.md](CHANGELOG.md) for full release details)

### Architecture

`SecureCore` enforces a strict **API / Internal / Factory** segregation across all modules:
* **`api/`**: Public interfaces (`PassphraseManager`, `SecurePreferences`, `SecureFileStorage`, `BiometricAuthManager`, `IntegrityManager`, `TamperProtectionManager`, `ScreenProtectionManager`, `NetworkSecurityManager`, `CryptoManager`, `AsymmetricCryptoManager`) and strongly-typed models (`BiometricResult`, `BiometricStatus`, `IntegrityReport`, `ThreatType`, `AppSignatureStatus`). Consumers only interact with these contracts.
* **`internal/`**: All implementations (`*Impl`, `RootDetector`, `EmulatorDetector`, `HookDetector`) are encapsulated using Kotlin's `internal` visibility modifier, protecting developers from breaking platform changes and isolating third-party/OS dependencies.
* **`SecureCore` (Factory)**: Single unified facade providing factory initializers (`createPassphraseManager()`, `createSecurePreferences()`, `createBiometricAuthManager()`, `createIntegrityManager()`, `createTamperProtectionManager()`, `createScreenProtectionManager()`, `createNetworkSecurityManager()`, `createCryptoManager()`, `createAsymmetricCryptoManager()`, etc.) and hardware integrity helpers.

### Features
- **Keystore-Backed Master Key**: Integrates with the Android Keystore using `AES-256 GCM` (hardware-backed on devices with a TEE or StrongBox).
- **`PassphraseManager`**: Generates and persists high-entropy passphrases encrypted on disk. It automatically discards `0x00` (null bytes), guaranteeing direct compatibility with native database engines like SQLCipher.
- **`SecurePreferences`**: Type-safe encrypted key-value storage powered by `EncryptedSharedPreferences` (`AES-256 SIV` for keys and `AES-256 GCM` for values) with out-of-the-box JSON serialization for custom models via Gson.
- **`SecureFileStorage`**: High-performance encrypted file operations running off the main thread (`Dispatchers.IO`) through Kotlin Coroutines.
- **Device & Runtime Integrity (`api/integrity/IntegrityManager`)**:
  - **Root Detection**: Heuristic evaluation for root management apps, test-keys signatures, dangerous mount paths, and common su binaries (`/system/xbin/su`, `/sbin/su`, etc.).
  - **Emulator & Virtual Environment Detection**: Identification of emulated environments (AVD, Genymotion, QEMU) inspecting build properties, hardware fingerprints, and communication pipes.
  - **Dynamic Hooking & Debugger Detection**: Memory map introspection (`/proc/self/maps`) detecting Frida/Xposed frameworks, package debuggable flags, and active debugger attachment (`android.os.Debug.isDebuggerConnected()`).
  - **Structured Integrity Reporting**: Multi-threat classification exposing aggregated statuses (`isDeviceCompromised()`) and detailed diagnostic reports (`getIntegrityReport()`).
- **APK Tamper & Signature Protection (`api/tamper/TamperProtectionManager`)**:
  - **Signature Hash Verification**: Runtime SHA-256 fingerprint validation of the active X.509 certificate to detect repackaging, tampering, and re-signing.
  - **Installer Source Verification**: Inspects installation origin (e.g. Google Play Store vs untrusted sideloading).
  - **Consolidated Diagnostic Status**: Combines certificate fingerprint and installer trust through `AppSignatureStatus` (`isGenuine`).
- **Screen & Window Privacy (`api/screen/ScreenProtectionManager`)**:
  - **Screenshot & Recording Blocking**: Dynamic application of `WindowManager.LayoutParams.FLAG_SECURE` blocking screenshots and screen recording through standard Android mechanisms (it does not stop capture by a rooted device, external hardware, or a photo of the screen).
  - **Recents / App Switcher Obscuration**: Automatic window preview blanking in the Android multitask switcher to protect sensitive views.
  - **Jetpack Compose Lifecycle Integration (`SecureScreenEffect`)**: Automatic declarative enablement while composable screens or modal dialogs remain in composition, reverting to previous states on disposal.
- **Network Security & MitM Protection (`api/network/NetworkSecurityManager`)**:
  - **SSL/TLS Certificate Pinning**: OkHttp `CertificatePinner` generation and automated client builder binding with SHA-256 public key pins, mitigating certificate-substitution Man-in-the-Middle (MitM) attacks. Always configure a backup pin; a wrong or expired pin blocks all connections to that host.
  - **Untrusted / User CA Detection**: Introspects `AndroidCAStore` for user-installed root certificates (`user:` prefix) commonly injected by traffic capture proxies (Charles Proxy, Burp Suite, mitmproxy).
- **Keystore-Backed AES-256-GCM (`api/crypto/CryptoManager`)**:
  - Direct cryptographic operations over arbitrary `ByteArray` payloads.
  - Streaming encryption and decryption via `InputStream` and `OutputStream` designed for large files without high memory pressure.
  - Encapsulated framing: automatic 12-byte random IV initialization, 1-byte header, and 128-bit GCM authentication tag verification.
  - Strongly-typed `CryptoException` hierarchy for explicit domain error handling.
- **Asymmetric Cryptography & Digital Signatures (`api/crypto/AsymmetricCryptoManager`)**:
  - RSA key pairs (encryption, decryption and signatures) and EC P-256 key pairs (signatures only), generated inside the AndroidKeyStore. The private key never leaves the Keystore; it is hardware-backed (TEE) only on devices that support it. StrongBox is not requested.
  - RSA encryption uses `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` with MGF1-SHA1, as required by the Keystore. Maximum plaintext is about 190 bytes with RSA-2048; for larger data use hybrid encryption (for example, protect the data with `CryptoManager` and encrypt only that key with RSA). Any backend encrypting for this key must use the same OAEP parameters.
  - Signatures with `SHA256withRSA` (PKCS#1 v1.5) and `SHA256withECDSA`. `verify()` returns `false` for invalid or malformed signatures.
  - X.509/DER public key export (`SubjectPublicKeyInfo`) via `getPublicKeyEncoded()`.
  - One key pair per alias is used for all operations. To keep encryption and signing keys separate, use two aliases. If an alias already holds a different key type or size, creation fails with `IllegalStateException`.
  - Keys are not bound to user authentication, and `deleteKey()` makes the instance unusable.
  - Operations throw exceptions. The first use of an alias generates the key pair, which can be slow: create the manager from a background thread.
- **Biometric Authentication (`api/biometric/BiometricAuthManager`)**:
  - Unified hardware readiness and enrollment verification via `canAuthenticate()`.
  - Biometric dialog orchestration with optional device credential fallback (PIN, pattern, password).
  - Cryptographic-bound authentication (`authenticateWithCrypto()`) backed by `BiometricPrompt.CryptoObject` and the Android Keystore.
  - Exhaustive sealed result models (`BiometricResult.Success`, `BiometricResult.Error`, `BiometricResult.Failed`).
- **StrongBox Detection**: Detects StrongBox Keymaster support on compatible hardware (Android 9.0+ / API 28+).
- **Explicit Error Handling**: Storage APIs return Kotlin `Result<T>`, biometrics report outcomes through the sealed `BiometricResult`, and the crypto managers (`CryptoManager`, `AsymmetricCryptoManager`) signal failures with exceptions that you must handle.

### Important Notice & Disclaimer

> **Please read before using this library in production.**

- **AI-assisted development:** This project was developed with the assistance of AI coding tools. It has been tested by the author, but it has not been exhaustively reviewed line by line and may contain errors.
- **No independent security audit:** `SecureCore` has not been audited by a third-party security professional. Do not assume it is free of vulnerabilities. Perform your own review, testing and threat modeling before relying on it.
- **Hardware backing varies by device:** Keys are stored in the Android Keystore, but whether they live in secure hardware (TEE or StrongBox) depends on the device. Do not assume hardware protection on every device.
- **Detection is heuristic, not absolute:** Root, emulator, hooking/debugger and tamper detection are best-effort heuristics. A determined attacker (e.g. using Frida or Magisk with hiding modules) may bypass them. Use them as one layer of a defense-in-depth strategy, never as the only protection, and always validate critical decisions server-side.
- **No warranty, no liability:** This software is provided "AS IS", without warranty of any kind, as stated in the [MIT License](LICENSE). The author is not liable for any damages, data loss, security incidents or other consequences arising from its use.
- **Your responsibility:** You are responsible for verifying that this library meets your security, legal and regulatory requirements (e.g. GDPR, PCI-DSS, export rules on cryptography) in your specific use case.
- **Found a vulnerability?** Please report it privately as described in [SECURITY.md](SECURITY.md).

### Installation

1. Add the JitPack repository in your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2. Add the dependency in your application module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.jragcer:secure-core-android:1.7.0")
    
    // Coroutines support for asynchronous file storage
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Fragment KTX required by AndroidX BiometricPrompt hosting activity
    implementation("androidx.fragment:fragment-ktx:1.8.6")
}
```

Ensure your project targets `minSdk = 24` or higher and includes Internet permission when testing network features:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Quick Start

#### 1. Generating a Safe Passphrase (e.g. for SQLCipher)
```kotlin
import com.github.jragcer.securitycore.SecureCore

val passphraseManager =  SecureCore.createPassphraseManager(context) 

passphraseManager.getOrCreatePassphrase("demo_passphrase_key")
  .onSuccess { keyBytes ->
    // Pass keyBytes directly to SQLCipher or Realm
  }
  .onFailure { error ->
    // Handle cryptographic initialization or disk I/O errors
  }
```

#### 2. Encrypted Key-Value Storage (`SecurePreferences`)
```kotlin
import com.github.jragcer.securitycore.SecureCore

val securePreferences =  SecureCore.createSecurePreferences(context).getOrNull()

securePreferences?.putString( "session_token", "encrypted_jwt_token_sample_12345" )
val retrieved = securePreferences?.getString("session_token")
```

#### 3. Encrypted File Persistence (`SecureFileStorage`)
```kotlin
import androidx.lifecycle.lifecycleScope
import com.github.jragcer.securitycore.SecureCore
import kotlinx.coroutines.launch

val secureFileStorage =  SecureCore.createSecureFileStorage(context)

lifecycleScope.launch {
  val fileName = "demo_secure_note.txt"
  val content = "Confidential payload secured with EncryptedFile."

  // Encrypted Write
  secureFileStorage.writeText(fileName, content)
    .onSuccess {
      // File encrypted and written to disk
    }
    .onFailure {
      // Handle write/encryption failure
    }

  // Decrypted Read
  secureFileStorage.readText(fileName)
    .onSuccess { decryptedText ->
      // Use decrypted content safely
    }
    .onFailure { exception ->
      // Handle I/O or decryption errors
    }
}
```

#### 4. StrongBox Hardware Check
```kotlin
import com.github.jragcer.securitycore.SecureCore

val hasStrongBox = SecureCore.isStrongBoxSupported(context)
```

#### 5. Biometric Authentication
```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.biometric.BiometricResult
import com.github.jragcer.securitycore.api.biometric.BiometricStatus

val biometricManager = SecureCore.createBiometricAuthManager(context)

when (val status = biometricManager.canAuthenticate(allowDeviceCredential = true)) {
    is BiometricStatus.Ready -> {
        biometricManager.authenticate(
            activity = this, // FragmentActivity
            title = "Biometric Verification",
            subtitle = "Authenticate to unlock secure operations",
            allowDeviceCredential = true
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    // Access granted
                }
                is BiometricResult.Error -> {
                    // Handle error: result.errorCode, result.errorMessage
                }
                is BiometricResult.Failed -> {
                    // Biometric rejected
                }
            }
        }
    }
    is BiometricStatus.NotEnrolled -> {
        // Prompt user to register credentials in System Settings
    }
    else -> {
        // Biometrics unavailable or unsupported
    }
}
```

#### 6. Device Integrity & Runtime Threat Assessment
```kotlin
import com.github.jragcer.securitycore.SecureCore

val integrityManager = SecureCore.createIntegrityManager(context)

// Fast boolean check for hostile environments
if (integrityManager.isDeviceCompromised()) {
  // Device is rooted, emulated, or under active debugging/hooking
}

// Detailed diagnostic report
val report = integrityManager.getIntegrityReport()
if (!report.isSecure) {
  println("Active threats: ${report.detectedThreats.joinToString()}")
  report.details.forEach { (check, detected) ->
    println("• $check: $detected")
  }
}
```

#### 7. APK Tamper & Signature Protection
```kotlin
import com.github.jragcer.securitycore.SecureCore

val tamperManager = SecureCore.createTamperProtectionManager(context)

// Replace with the SHA-256 fingerprint of YOUR release certificate
// (see `./gradlew signingReport`). The expected value ships inside the APK, so a
// determined attacker can patch this check: treat it as one layer, not a guarantee.
val expectedSignature = "AA:BB:CC:DD:00:11:22:33:44:55:66:77:88:99:EE:FF..."
val isGenuineCert = tamperManager.verifySignature(expectedSignature)

// Consolidated report (Signature validity + Official store installer)
val status = tamperManager.getSignatureStatus(expectedHash = expectedSignature)
if (!status.isGenuine) {
  println("Tampered or untrusted build detected!")
  println("Signature valid: ${status.isSignatureValid}")
  println("Installer source: ${status.installerPackageName ?: "Sideloaded"}")
}
```
#### 8. Screen & Window Protection (`FLAG_SECURE`)
```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.screen.SecureScreenEffect

// Imperative Activity-level control
val screenManager = SecureCore.createScreenProtectionManager()
screenManager.enableProtection(activity)
val isSecured = screenManager.isProtectionActive(activity)
screenManager.disableProtection(activity)

// Declarative Compose integration
@Composable
fun SensitivePaymentScreen() {
    // Automatically applies FLAG_SECURE while in composition, restores on disposal
    SecureScreenEffect(enabled = true)

    // Protected UI content
}
```
#### 9. Network Security, Certificate Pinning & MitM Detection
```kotlin
import com.github.jragcer.securitycore.SecureCore
import okhttp3.OkHttpClient

val networkManager = SecureCore.createNetworkSecurityManager(context)

// Detect suspicious user-installed CAs (e.g., Charles Proxy / Burp Suite)
val hasSuspiciousProxy = networkManager.hasUserInstalledCertificates()

// Replace with your real pins. A wrong or expired pin blocks every connection to that host,
// so always include a backup pin.
val pins = mapOf(
    "api.example.com" to listOf(
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Primary pin
        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup pin
    )
)

val client = networkManager.applyPinning(OkHttpClient.Builder(), pins).build()
```

#### 10. Hardware-Backed Encryption & Streaming (`CryptoManager`)
```kotlin
import com.github.jragcer.securitycore.SecureCore

val cryptoManager = SecureCore.createCryptoManager(keyAlias = "AppMasterKey")

// 1. Arbitrary byte array encryption
val plaintext = "Sensitive data payload".toByteArray(Charsets.UTF_8)
val encryptedBytes = cryptoManager.encrypt(plaintext)
val decryptedBytes = cryptoManager.decrypt(encryptedBytes)

// 2. Large file/stream encryption without loading into RAM
cryptoManager.encrypt(fileInputStream, fileOutputStream)
cryptoManager.decrypt(fileInputStream, fileOutputStream)
```

#### 11. Asymmetric Cryptography & Signatures

```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm

// Create managers from a background thread: the first use of an alias generates the key pair.

// 1. RSA (2048-bit, OAEP) for encryption. Use a dedicated alias for it.
val rsaManager = SecureCore.createAsymmetricCryptoManager("ClientEncryptionKey")

// 2. Encrypt with the public key, decrypt with the Keystore-held private key.
//    Limit: ~190 bytes with RSA-2048. For larger data use hybrid encryption.
val ciphertext = rsaManager.encrypt("Confidential Token".toByteArray())
val decrypted = rsaManager.decrypt(ciphertext)

// 3. A separate alias for signing (EC P-256 here).
val signer = SecureCore.createAsymmetricCryptoManager("ClientSigningKey", AsymmetricAlgorithm.EC())
val transactionData = "Transfer \$500 to Account B".toByteArray()
val signature = signer.sign(transactionData)
val isValid = signer.verify(transactionData, signature) // false for invalid or malformed signatures

// 4. Export the X.509 public key. Your backend needs it to verify signatures.
val x509PublicKeyBytes = signer.getPublicKeyEncoded()
```

### Testing
Run unit tests and instrumented tests via Gradle:

```bash
# Unit tests
./gradlew test

# Instrumented tests against real Keystore instances
./gradlew connectedAndroidTest
```

### Architecture & Security Overview

| Component              | Underlying Technology                                                   | Cryptographic Scheme                                                                |
|:-----------------------|:------------------------------------------------------------------------|:------------------------------------------------------------------------------------|
| **Passphrase Engine**  | `EncryptedFile` + Android Keystore                                      | `AES-256 GCM HKDF 4KB`                                                              |
| **Preference Engine**  | `EncryptedSharedPreferences`                                            | Keys: `AES-256 SIV`<br>Values: `AES-256 GCM`                                        |
| **File Engine**        | `EncryptedFile` + IO Streams                                            | `AES-256 GCM HKDF 4KB`                                                              |
| **Biometric Engine**   | AndroidX `BiometricPrompt`                                              | Hardware-backed Biometric / CryptoObject                                            |
| **Integrity Engine**   | Multi-heuristic detection ( `su`,`/proc/self/maps`,`Build properties` ) | Runtime Anti-Tamper & Environment Analysis                                          |
| **Tamper Protection**  | `SigningInfo`/`PackageManager` + `MessageDigest`                        | SHA-256 Certificate Fingerprinting & Origin Check                                   |
| **Screen Protection**  | `WindowManager.LayoutParams.FLAG_SECURE`                                | OS Window Privacy Buffer Masking                                                    |
| **Network Security**   | OkHttp `CertificatePinner` + `AndroidCAStore`                           | Public Key SHA-256 Hash Pinning & Root CA Audit                                     |
| **Keystore Crypto**    | `AndroidKeyStore` + AES-GCM Cipher                                      | `AES-256 GCM NoPadding` (hardware-backed when available)                            |
| **Asymmetric Crypto**  | `AndroidKeyStore` + RSA/EC KeyPair                                      | RSA OAEP (SHA-256, MGF1-SHA1) + RSA PKCS#1 v1.5 signatures; ECDSA P-256 signatures  |

### License
Distributed under the Apache License, Version 2.0. See [`LICENSE`](LICENSE) for more information.

---

<a name="español"></a>
## Español

`SecureCore` es un SDK de seguridad ligero para Android diseñado para simplificar la criptografía respaldada por Keystore (AES-256-GCM, además de pares de claves RSA/EC y firmas digitales), el almacenamiento local seguro, la autenticación biométrica, la evaluación heurística de integridad en tiempo de ejecución, la detección de manipulación del APK, el bloqueo de capturas de pantalla, la fijación de certificados SSL/TLS y la detección de CAs instaladas por el usuario (señal habitual de proxies de interceptación), con el mínimo boilerplate. Las claves están respaldadas por hardware solo en los dispositivos que lo soportan.
> **Versión actual:** `1.7.0` (Consulta [CHANGELOG.md](CHANGELOG.md) para ver los detalles de la versión)

### Arquitectura

`SecureCore` aplica una estricta segregación bajo el patrón **API / Internal / Factory** en todos sus módulos:
- **`api/`**: Interfaces públicas (`PassphraseManager`, `SecurePreferences`, `SecureFileStorage`, `BiometricAuthManager`, `IntegrityManager`, `TamperProtectionManager`, `ScreenProtectionManager`, `NetworkSecurityManager`, `CryptoManager`, `AsymmetricCryptoManager`) y modelos fuertemente tipados (`BiometricResult`, `BiometricStatus`, `IntegrityReport`, `ThreatType`, `AppSignatureStatus`). Los consumidores únicamente interactúan con estos contratos.
- **`internal/`**: Todas las implementaciones (`*Impl`, `RootDetector`, `EmulatorDetector`, `HookDetector`) quedan encapsuladas mediante el modificador de visibilidad `internal` de Kotlin, protegiendo a los desarrolladores de cambios de plataforma que rompan compatibilidad y aislando las dependencias del sistema operativo y de terceros.
- **`SecureCore` (Factoría)**: Fachada única y unificada que provee métodos factoría de inicialización (`createPassphraseManager()`, `createSecurePreferences()`, `createBiometricAuthManager()`, `createIntegrityManager()`, `createTamperProtectionManager()`, `createScreenProtectionManager()`, `createNetworkSecurityManager()`, `createCryptoManager()`, `createAsymmetricCryptoManager()`, etc.) y herramientas de validación de integridad del hardware.

### Características
- **Clave maestra en Keystore**: Se integra con Android Keystore mediante `AES-256 GCM` (respaldada por hardware en dispositivos con TEE o StrongBox).
- **`PassphraseManager`**: Genera y persiste frases de contraseña con alta entropía cifradas en disco. Descarta automáticamente los bytes nulos (`0x00`), asegurando compatibilidad directa con motores de base de datos nativos como SQLCipher.
- **`SecurePreferences`**: Almacenamiento clave-valor cifrado con tipado estricto mediante `EncryptedSharedPreferences` (`AES-256 SIV` para claves y `AES-256 GCM` para valores), con serialización JSON integrada para modelos personalizados con Gson.
- **`SecureFileStorage`**: Operaciones seguras sobre ficheros ejecutadas fuera del hilo principal (`Dispatchers.IO`) mediante corrutinas de Kotlin.
- **Integridad del Dispositivo y Entorno de Ejecución (`api/integrity/IntegrityManager`)**:
  - Detección de Root: Análisis heurístico de aplicaciones de gestión de superusuario, firmas `test-keys`, puntos de montaje sospechosos y binarios `su` habituales (`/system/xbin/su`, `/sbin/su`, etc.).
  - Detección de Emuladores y Entornos Virtuales: Identificación de entornos virtualizados (AVD, Genymotion, QEMU) mediante huellas en propiedades del sistema (`Build`), hardware y canales de comunicación simulados.
  - Detección de Depuración y Hooking Dinámico: Escaneo del mapa de memoria de la app (`/proc/self/maps`) en busca de librerías de Frida/Xposed, estado de depuración en manifiesto (`debuggable`) y depuradores activos (`android.os.Debug.isDebuggerConnected()`).
  - Informes Estructurados de Amenazas: Clasificación de riesgos mediante evaluación rápida (`isDeviceCompromised()`) o informes desglosados (`getIntegrityReport()`).
- **Protección Anti-Tamper y Firma de APK (`api/tamper/TamperProtectionManager`)**:
  - **Validación de Firma en Runtime**: Cotejo del hash SHA-256 del certificado X.509 activo contra firmas esperadas para detectar APKs recompilados o refirmados.
  - **Verificación del Instalado**: Comprobación del origen de instalación (Google Play Store frente a orígenes desconocidos / sideloading).
  - **Diagnóstico Consolidado**: Estado unificado mediante `AppSignatureStatus` y propiedad calculada `isGenuine`.
- **Protección de Pantalla y Ventana (`api/screen/ScreenProtectionManager`)**:
  - **Bloqueo de Capturas y Grabaciones**: Aplicación dinámica de `WindowManager.LayoutParams.FLAG_SECURE` ppara bloquear capturas y grabaciones mediante los mecanismos estándar de Android (no impide la captura en un dispositivo rooteado, con hardware externo ni con una foto de la pantalla).
  - **Ocultación en Multitarea / Recientes**: Oculta la vista previa de la app en el selector de aplicaciones recientes del sistema Android.
  - **Integración con Jetpack Compose (`SecureScreenEffect`)**: Efecto de ciclo de vida declarativo (`DisposableEffect`) que protege automáticamente composables o diálogos mientras permanezcan en composición y restaura el estado al desmontarse.
- **Seguridad de Red y Protección MitM (`api/network/NetworkSecurityManager`)**:
  - **Fijación de Certificados SSL/TLS (Certificate Pinning)**: Generación y vinculación automatizada de `CertificatePinner` en `OkHttpClient.Builder` con hashes de clave pública SHA-256 para mitigar la intercepción de tráfico. Configura siempre un pin de respaldo: un pin incorrecto o caducado bloquea todas las conexiones a ese host.
  - **Detección de CAs de Usuario / Proxies**: Análisis del almacén `AndroidCAStore` para detectar certificados raíz instalados por el usuario o perfiles MDM (prefijo `user:`), habituales en herramientas de interceptación (Charles Proxy, Burp Suite, mitmproxy).
- **Criptografía AES-256-GCM respaldada por Keystore (`api/crypto/CryptoManager`)**:
  - Operaciones criptográficas autenticadas directas sobre arrays de bytes arbitrarios (`ByteArray`).
  - Cifrado y descifrado por flujos (`InputStream` y `OutputStream`) diseñado para procesar archivos grandes sin sobrecargar la memoria RAM.
  - Estructuración transparente: IV aleatorio de 12 bytes generado por hardware, cabecera de 1 byte y verificación de tag de autenticación GCM de 128 bits.
  - Jerarquía sellada `CryptoException` para control explícito de fallos criptográficos.
- **Criptografía Asimétrica y Firmas Digitales (`api/crypto/AsymmetricCryptoManager`)**:
  - Pares de claves RSA (cifrado, descifrado y firmas) y EC P-256 (solo firmas), generados dentro del AndroidKeyStore. La clave privada nunca sale del Keystore; está respaldada por hardware (TEE) solo en dispositivos que lo soportan. No se solicita StrongBox.
  - El cifrado RSA usa `RSA/ECB/OAEPWithSHA-256AndMGF1Padding` con MGF1-SHA1, como exige el Keystore. El máximo en claro es de unos 190 bytes con RSA-2048; para datos mayores usa cifrado híbrido (por ejemplo, protege los datos con `CryptoManager` y cifra con RSA solo esa clave). Cualquier backend que cifre para esta clave debe usar los mismos parámetros OAEP.
  - Firmas con `SHA256withRSA` (PKCS#1 v1.5) y `SHA256withECDSA`. `verify()` devuelve `false` si la firma es inválida o está malformada.
  - Exportación de la clave pública en X.509/DER (`SubjectPublicKeyInfo`) mediante `getPublicKeyEncoded()`.
  - Se usa un único par de claves por alias para todas las operaciones. Para separar las claves de cifrado y firma, usa dos alias. Si un alias ya contiene otro tipo o tamaño de clave, la creación falla con `IllegalStateException`.
  - Las claves no están vinculadas a la autenticación del usuario, y `deleteKey()` deja la instancia inutilizable.
  - Las operaciones lanzan excepciones. El primer uso de un alias genera el par de claves, que puede tardar: crea el manager desde un hilo en segundo plano.
- **Autenticación Biométrica (`api/biometric/BiometricAuthManager`)**:
  - Comprobación de disponibilidad de hardware y credenciales registradas mediante `canAuthenticate()`.
  - Despliegue de prompts biométricos nativos con soporte para credenciales del dispositivo (PIN, patrón, contraseña).
  - Autenticación vinculada a operaciones criptográficas (`authenticateWithCrypto()`) mediante `BiometricPrompt.CryptoObject` y Android Keystore.
  - Modelado tipado exhaustivo (`BiometricResult.Success`, `BiometricResult.Error`, `BiometricResult.Failed`).
- **Detección de StrongBox**: Detecta la disponibilidad de StrongBox Keymaster en hardware compatible (Android 9.0+ / API 28+).
- **Manejo explícito de errores**: Las APIs de almacenamiento devuelven `Result<T>` de Kotlin, la biometría informa mediante el sellado `BiometricResult` y los gestores criptográficos (`CryptoManager`, `AsymmetricCryptoManager`) señalan los fallos con excepciones que debes gestionar.

### Aviso importante y exención de responsabilidad

> **Léelo antes de usar esta librería en producción.**

- **Desarrollo asistido por IA:** Este proyecto se ha desarrollado con ayuda de herramientas de IA para programar. El autor lo ha probado, pero no se ha revisado exhaustivamente línea a línea y puede contener errores.
- **Sin auditoría de seguridad independiente:** `SecureCore` no ha sido auditada por un profesional de seguridad externo. No des por hecho que está libre de vulnerabilidades. Realiza tu propia revisión, pruebas y modelado de amenazas antes de depender de ella.
- **El respaldo por hardware depende del dispositivo:** Las claves se guardan en el Android Keystore, pero que residan en hardware seguro (TEE o StrongBox) depende del dispositivo. No des por hecha la protección por hardware en todos los dispositivos.
- **La detección es heurística, no absoluta:** La detección de root, emuladores, hooking/depuradores y manipulación del APK se basa en heurísticas de mejor esfuerzo. Un atacante con determinación (por ejemplo, usando Frida o Magisk con módulos de ocultación) puede eludirlas. Úsalas como una capa más dentro de una estrategia de defensa en profundidad, nunca como única protección, y valida siempre las decisiones críticas en el servidor.
- **Sin garantía ni responsabilidad:** Este software se proporciona "TAL CUAL", sin garantía de ningún tipo, según establece la [licencia MIT](LICENSE). El autor no se hace responsable de daños, pérdida de datos, incidentes de seguridad u otras consecuencias derivadas de su uso.
- **Tu responsabilidad:** Eres responsable de comprobar que esta librería cumple tus requisitos de seguridad, legales y normativos (p. ej. RGPD, PCI-DSS, normativa de exportación de criptografía) en tu caso de uso concreto.
- **¿Has encontrado una vulnerabilidad?** Infórmala de forma privada como se indica en [SECURITY.md](SECURITY.md).

### Instalación
1. Añade el repositorio de JitPack en tu archivo raíz `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2. Añade la dependencia en el `build.gradle.kts` de tu aplicación:

```kotlin
dependencies {
    implementation("com.github.jragcer:secure-core-android:1.7.0")
    
    // Soporte de corrutinas para operaciones asíncronas de ficheros
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // Fragment KTX requerido por la actividad anfitriona de BiometricPrompt
    implementation("androidx.fragment:fragment-ktx:1.8.6")
}
```

Asegúrate de que tu proyecto tenga como target `minSdk = 24` o superior e incluya el permiso de acceso a Internet al probar funcionalidades de red:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

### Guía rápida de uso

#### 1. Generar una clave segura (ej. para SQLCipher)
```kotlin
import com.github.jragcer.securitycore.SecureCore

val passphraseManager =  SecureCore.createPassphraseManager(context)

passphraseManager.getOrCreatePassphrase("demo_passphrase_key")
  .onSuccess { keyBytes ->
    // Pasa keyBytes directamente a SQLCipher o Realm
  }
  .onFailure { error ->
    // Manejo de errores criptográficos o de entrada/salida
  }
```

#### 2. Almacenamiento clave-valor cifrado (`SecurePreferences`)
```kotlin
import com.github.jragcer.securitycore.SecureCore
        
val securePreferences =  SecureCore.createSecurePreferences(context).getOrNull() 

securePreferences?.putString( "session_token", "encrypted_jwt_token_sample_12345" )
val retrieved = securePreferences?.getString("session_token")
```

#### 3. Persistencia de ficheros cifrados (`SecureFileStorage`)
```kotlin
import androidx.lifecycle.lifecycleScope
import com.github.jragcer.securitycore.SecureCore
import kotlinx.coroutines.launch

val secureFileStorage =  SecureCore.createSecureFileStorage(context)

lifecycleScope.launch {
  val fileName = "demo_secure_note.txt"
  val content = "Confidential payload secured with EncryptedFile."

  // Escritura cifrada
  secureFileStorage.writeText(fileName, content)
    .onSuccess {
      // Archivo cifrado y persistido en disco
    }
    .onFailure {
      // Manejo de fallos de escritura o cifrado
    }

  // Lectura descifrada
  secureFileStorage.readText(fileName)
    .onSuccess { decryptedText ->
      // Uso seguro del contenido en texto plano
    }
    .onFailure { exception ->
      // Manejo de fallos de lectura o descifrado
    }
}
```

#### 4. Comprobación de hardware StrongBox
```kotlin
import com.github.jragcer.securitycore.SecureCore

val soportaStrongBox = SecureCore.isStrongBoxSupported(context)
```

#### 5. Autenticación Biométrica
```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.biometric.BiometricResult
import com.github.jragcer.securitycore.api.biometric.BiometricStatus

val biometricManager = SecureCore.createBiometricAuthManager(context)

when (val status = biometricManager.canAuthenticate(allowDeviceCredential = true)) {
    is BiometricStatus.Ready -> {
        biometricManager.authenticate(
            activity = this, // FragmentActivity
            title = "Verificación Biométrica",
            subtitle = "Autentícate para realizar operaciones seguras",
            allowDeviceCredential = true
        ) { result ->
            when (result) {
                is BiometricResult.Success -> {
                    // Acceso concedido
                }
                is BiometricResult.Error -> {
                    // Gestión de errores: result.errorCode, result.errorMessage
                }
                is BiometricResult.Failed -> {
                    // Huella o rostro no reconocido
                }
            }
        }
    }
    is BiometricStatus.NotEnrolled -> {
        // Redirigir a Ajustes para registrar credenciales
    }
    else -> {
        // Biometría no disponible o no compatible
    }
}
```

#### 6. Evaluación de Integridad y Detección de amenazas.
```kotlin
import com.github.jragcer.securitycore.SecureCore

val integrityManager = SecureCore.createIntegrityManager(context)

// Verificación rápida booleana de entorno hostil
if (integrityManager.isDeviceCompromised()) {
  // El dispositivo está rooteado, es un emulador o tiene un depurador/hooking activo
}

// Informe diagnóstico detallado
val report = integrityManager.getIntegrityReport()
if (!report.isSecure) {
  println("Amenazas detectadas: ${report.detectedThreats.joinToString()}")
  report.details.forEach { (prueba, detectada) ->
    println("• $prueba: $detectada")
  }
}
```

#### 7. Protección Anti-Tamper y Validación de Firma de APK.
```kotlin
import com.github.jragcer.securitycore.SecureCore

val tamperManager = SecureCore.createTamperProtectionManager(context)

// Sustituye por la huella SHA-256 del certificado de TU release (ver ./gradlew signingReport). 
// El valor esperado viaja dentro del APK, así que un atacante decidido puede parchear esta comprobación: 
// úsala como una capa más, no como garantía.
val huellaEsperada = "AA:BB:CC:DD:00:11:22:33:44:55:66:77:88:99:EE:FF..."
val firmaValida = tamperManager.verifySignature(huellaEsperada)

// Informe de estado consolidado (Firma y tienda oficial)
val status = tamperManager.getSignatureStatus(expectedHash = huellaEsperada)
if (!status.isGenuine) {
  println("¡APK manipulada o instalada de origen no verificado!")
  println("Firma válida: ${status.isSignatureValid}")
  println("Instalador: ${status.installerPackageName ?: "Sideloaded / Desconocido"}")
}
```

#### 8. Protección de Pantalla y Ventana (`FLAG_SECURE`)
```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.screen.SecureScreenEffect

// Control imperativo por Activity
val screenManager = SecureCore.createScreenProtectionManager()
screenManager.enableProtection(activity)
val isSecured = screenManager.isProtectionActive(activity)
screenManager.disableProtection(activity)

// Integración declarativa en Jetpack Compose
@Composable
fun SensitivePaymentScreen() {
    // Activa FLAG_SECURE automáticamente mientras el composable esté visible
    SecureScreenEffect(enabled = true)

    // Contenido protegido
}
```

#### 9. Seguridad de Red, Certificate Pinning y Detección MitM
```kotlin
import com.github.jragcer.securitycore.SecureCore
import okhttp3.OkHttpClient

val networkManager = SecureCore.createNetworkSecurityManager(context)

// Detectar CAs sospechosas instaladas por el usuario (Charles/Burp)
val proxyDetectado = networkManager.hasUserInstalledCertificates()

// Sustituye por tus pines reales. Un pin incorrecto o caducado bloquea todas las conexiones a ese host,
// así que incluye siempre un pin de respaldo
val pines = mapOf(
    "api.midominio.com" to listOf(
        "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="
    )
)

val client = networkManager.applyPinning(OkHttpClient.Builder(), pines).build()
```

#### 10. Criptografía directa por hardware y Streaming (`CryptoManager`)
```kotlin
import com.github.jragcer.securitycore.SecureCore

val cryptoManager = SecureCore.createCryptoManager(keyAlias = "AppMasterKey")

// 1. Cifrado y descifrado directo de arrays de bytes
val textoPlano = "Datos sensibles en memoria".toByteArray(Charsets.UTF_8)
val datosCifrados = cryptoManager.encrypt(textoPlano)
val datosDescifrados = cryptoManager.decrypt(datosCifrados)

// 2. Cifrado en streaming de archivos grandes sin consumir RAM
cryptoManager.encrypt(ficheroInputStream, ficheroOutputStream)
cryptoManager.decrypt(ficheroInputStream, ficheroOutputStream)
```

#### 11. Criptografía asimétrica y firmas

```kotlin
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm

// Crea los managers desde un hilo en segundo plano: el primer uso de un alias genera el par de claves.

// 1. RSA (2048 bits, OAEP) para cifrado. Usa un alias dedicado.
val rsaManager = SecureCore.createAsymmetricCryptoManager("ClientEncryptionKey")

// 2. Cifra con la clave pública y descifra con la clave privada custodiada en el Keystore.
//    Límite: ~190 bytes con RSA-2048. Para datos mayores usa cifrado híbrido.
val ciphertext = rsaManager.encrypt("Confidential Token".toByteArray())
val decrypted = rsaManager.decrypt(ciphertext)

// 3. Un alias distinto para firmar (aquí EC P-256).
val signer = SecureCore.createAsymmetricCryptoManager("ClientSigningKey", AsymmetricAlgorithm.EC())
val transactionData = "Transfer \$500 to Account B".toByteArray()
val signature = signer.sign(transactionData)
val isValid = signer.verify(transactionData, signature) // false si la firma es inválida o está malformada

// 4. Exporta la clave pública X.509. Tu backend la necesita para verificar firmas.
val x509PublicKeyBytes = signer.getPublicKeyEncoded()
```

### Pruebas (Testing)
Ejecuta la suite de pruebas unitarias o instrumentadas:

```bash
# Pruebas unitarias
./gradlew test

# Pruebas instrumentadas con Android Keystore real
./gradlew connectedAndroidTest
```

### Arquitectura y esquema de seguridad

| Componente                  | Tecnología base                                                               | Esquema criptográfico                                                              |
|:----------------------------|:------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------|
| **Motor de contraseñas**    | `EncryptedFile` + Android Keystore                                            | `AES-256 GCM HKDF 4KB`                                                             |
| **Motor de preferencias**   | `EncryptedSharedPreferences`                                                  | Claves: `AES-256 SIV`<br>Valores: `AES-256 GCM`                                    |
| **Motor de ficheros**       | `EncryptedFile` + Flujos de E/S                                               | `AES-256 GCM HKDF 4KB`                                                             |
| **Motor biométrico**        | AndroidX `BiometricPrompt`                                                    | Biometría respaldada por hardware / CryptoObject                                   |
| **Motor de integridad**     | Detección heurística múltiple ( `su`, `/proc/self/maps`, `propiedades Build`) | Análisis de entorno y mitigación de manipulación (Anti-Tamper)                     |
| **Protección Tamper**       | `SigningInfo`/`PackageManager` + `MessageDigest`                              | Huella SHA-256 de Certificados e Inspección de Origen                              |
| **Protección de Pantalla**  | `WindowManager.LayoutParams.FLAG_SECURE`                                      | Enmascaramiento de Buffer de Ventana a nivel SO                                    |
| **Seguridad de Red**        | OkHttp `CertificatePinner` + `AndroidCAStore`                                 | Fijación SHA-256 de Clave Pública y Auditoría de CAs Raíz                          |
| **Motor Criptográfico**     | `AndroidKeyStore` + Cifrador AES-GCM                                          | `AES-256 GCM NoPadding` (respaldado por hardware si está disponible)               |
| **Criptografía Asimétrica** | `AndroidKeyStore` + Par de Claves RSA/EC                                      | Cifrado RSA OAEP (SHA-256, MGF1-SHA1) y firmas RSA PKCS#1 v1.5; firmas ECDSA P-256 |

### Licencia
Distribuido bajo la Licencia Apache, Versión 2.0. Consulta el archivo [`LICENSE`](LICENSE) para obtener más información.

---
