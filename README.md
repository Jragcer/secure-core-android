# SecureCore Android Library

[![Android CI](https://github.com/Jragcer/secure-core-android/actions/workflows/build.yaml/badge.svg)](https://github.com/Jragcer/secure-core-android/actions/workflows/build.yaml)
[![Latest Release](https://img.shields.io/badge/Release-1.2.0-blue.svg)](https://github.com/jragcer/SecuritySdkDemo/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-brightgreen.svg)](https://android.com)

*Read this in [English](#english) | Léelo en [Español](#español)*

---

<a name="english"></a>
## English

`SecureCore` is an enterprise-grade Android security SDK engineered to simplify cryptography, secure local storage, biometric authentication, runtime integrity assessment, and hardware validations with minimal boilerplate.
> **Current Version:** `1.2.0` (See [CHANGELOG.md](CHANGELOG.md) for full release details)

### Architecture

`SecureCore` enforces a strict **API / Internal / Factory** segregation across all modules:
* **`api/`**: Public interfaces (`PassphraseManager`, `SecurePreferences`, `SecureFileStorage`, `BiometricAuthManager`, `IntegrityManager`) and strongly-typed models (`BiometricResult`, `BiometricStatus`, `IntegrityReport`, `ThreatType`). Consumers only interact with these contracts.
* **`internal/`**: All implementations (`*Impl`, `RootDetector`, `EmulatorDetector`, `HookDetector`) are encapsulated using Kotlin's `internal` visibility modifier, protecting developers from breaking platform changes and isolating third-party/OS dependencies.
* **`SecureCore` (Factory)**: Single unified facade providing factory initializers (`createPassphraseManager()`, `createSecurePreferences()`, `createBiometricAuthManager()`, `createIntegrityManager()`, etc.) and hardware integrity helpers.

### Features
* **Hardware-Backed Master Key**: Integrates transparently with the Android Keystore (TEE / StrongBox) using `AES-256 GCM`.
* **`PassphraseManagerImpl`**: Generates and persists high-entropy passphrases encrypted on disk. It automatically discards `0x00` (null bytes), guaranteeing direct compatibility with native database engines like SQLCipher.
* **`SecurePreferencesImpl`**: Type-safe encrypted key-value storage powered by `EncryptedSharedPreferences` (`AES-256 SIV` for keys and `AES-256 GCM` for values) with out-of-the-box JSON serialization for custom models via Gson.
* **`SecureFileStorageImpl`**: High-performance encrypted file operations running off the main thread (`Dispatchers.IO`) through Kotlin Coroutines.
* **Device & Runtime Integrity (`api/IntegrityManager`)**:
  * **Root Detection**: Heuristic evaluation for root management apps, test-keys signatures, dangerous mount paths, and common su binaries (`/system/xbin/su`, `/sbin/su`, etc.).
  * **Emulator & Virtual Environment Detection**: Identification of emulated environments (AVD, Genymotion, QEMU) inspecting build properties, hardware fingerprints, and communication pipes.
  * **Dynamic Hooking & Debugger Detection**: Memory map introspection (`/proc/self/maps`) detecting Frida/Xposed frameworks, package debuggable flags, and active debugger attachment (`android.os.Debug.isDebuggerConnected()`).
  * **Structured Integrity Reporting**: Multi-threat classification exposing aggregated statuses (`isDeviceCompromised()`) and detailed diagnostic reports (`getIntegrityReport()`).
* **Biometric Authentication (`api/BiometricAuthManager`)**:
    * Unified hardware readiness and enrollment verification via `canAuthenticate()`.
    * Biometric dialog orchestration with optional device credential fallback (PIN, pattern, password).
    * Cryptographic-bound authentication (`authenticateWithCrypto()`) backed by `BiometricPrompt.CryptoObject` and the Android Keystore.
    * Exhaustive sealed result models (`BiometricResult.Success`, `BiometricResult.Error`, `BiometricResult.Failed`).
* **Hardware Attestation**: StrongBox Keymaster detection on compatible hardware (Android 9.0+ / API 28+).
* **Resilient Error Handling**: Uses Kotlin's `Result<T>` pattern across all operations to prevent uncaught cryptographic crashes.


### Installation
Add the library module to your project's `settings.gradle.kts`:

```kotlin
include(":SecurityCore")
```

Then add the dependency in your application module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":SecurityCore"))
    
    // Coroutines support for asynchronous file storage
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Fragment KTX required by AndroidX BiometricPrompt hosting activity
    implementation("androidx.fragment:fragment-ktx:1.8.6")
}
```

Ensure your project targets `minSdk = 24` or higher.

### Quick Start

#### 1. Generating a Safe Passphrase (e.g. for SQLCipher)
```kotlin
import com.github.jragcer.securitycore.storage.PassphraseManager

val passphraseManager = PassphraseManager(context)

passphraseManager.getOrCreatePassphrase(filename = "db_passphrase.bin", length = 32)
    .onSuccess { keyBytes ->
        // Pass keyBytes directly to SQLCipher or Realm
    }
    .onFailure { error ->
        // Handle cryptographic initialization or disk I/O errors
    }
```

#### 2. Encrypted Key-Value Storage (`SecurePreferencesImpl`)
```kotlin
import com.github.jragcer.securitycore.storage.SecurePreferences

data class UserSession(val token: String, val role: String)

SecurePreferences.create(context, fileName = "app_secure_prefs")
    .onSuccess { prefs ->
        // Primitives
        prefs.putString("auth_token", "jwt_token_example")
        prefs.putBoolean("biometric_enabled", true)
        val token = prefs.getString("auth_token")

        // Complex Data Models (JSON serialized via Gson)
        val session = UserSession(token = "abc-123", role = "ADMIN")
        prefs.putObject("active_session", session)

        val savedSession = prefs.getObject<UserSession>("active_session")
    }
    .onFailure { error ->
        // Handle Keystore corruption or storage access issues
    }
```

#### 3. Encrypted File Persistence (`SecureFileStorageImpl`)
```kotlin
import androidx.lifecycle.lifecycleScope
import com.github.jragcer.securitycore.storage.SecureFileStorage
import kotlinx.coroutines.launch

val fileStorage = SecureFileStorage(context)

lifecycleScope.launch {
    val fileName = "confidential_notes.txt"

    // Encrypted Write
    fileStorage.writeText(fileName, "Confidential report content")
        .onSuccess {
            // File encrypted and written to disk
        }
        .onFailure { error ->
            // Handle write/encryption failure
        }

    // Decrypted Read
    fileStorage.readText(fileName)
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
import com.github.jragcer.securitycore.api.BiometricResult
import com.github.jragcer.securitycore.api.BiometricStatus

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

### Testing
Run unit tests and instrumented tests via Gradle:

```bash
# Unit tests
./gradlew test

# Instrumented tests against real Keystore instances
./gradlew connectedAndroidTest
```

### Architecture & Security Overview

| Component             | Underlying Technology                                                   | Cryptographic Scheme                         |
|:----------------------|:------------------------------------------------------------------------|:---------------------------------------------|
| **Passphrase Engine** | `EncryptedFile` + Android Keystore                                      | `AES-256 GCM HKDF 4KB`                       |
| **Preference Engine** | `EncryptedSharedPreferences`                                            | Keys: `AES-256 SIV`<br>Values: `AES-256 GCM` |
| **File Engine**       | `EncryptedFile` + IO Streams                                            | `AES-256 GCM HKDF 4KB`                       |
| **Biometric Engine**  | AndroidX `BiometricPrompt`                                              | Hardware-backed Biometric / CryptoObject     |
| **Integrity Engine**  | Multi-heuristic detection ( `su`,`/proc/self/maps`,`Build properties` ) | Runtime Anti-Tamper & Environment Analysis   |

### License
Distributed under the MIT License. See `LICENSE` for more information.

---

<a name="español"></a>
## Español

`SecureCore` es un SDK de seguridad para Android de nivel empresarial diseñado para simplificar tareas de criptografía, persistencia segura, autenticación biométrica, evaluación de integridad en tiempo de ejecución y validación de hardware con el mínimo código boilerplate.

> **Versión actual:** `1.2.0` (Consulta [CHANGELOG.md](CHANGELOG.md) para ver los detalles de la versión)

### Arquitectura

`SecureCore` aplica una estricta segregación bajo el patrón **API / Internal / Factory** en todos sus módulos:
- **`api/`**: Interfaces públicas (`PassphraseManager`, `SecurePreferences`, `SecureFileStorage`, `BiometricAuthManager`, `IntegrityManager`) y modelos fuertemente tipados (`BiometricResult`, `BiometricStatus`, `IntegrityReport`, `ThreatType`). Los consumidores únicamente interactúan con estos contratos.
- **`internal/`**: Todas las implementaciones (`*Impl`, `RootDetector`, `EmulatorDetector`, `HookDetector`) quedan encapsuladas mediante el modificador de visibilidad `internal` de Kotlin, protegiendo a los desarrolladores de cambios de plataforma que rompan compatibilidad y aislando las dependencias del sistema operativo y de terceros.
- **`SecureCore` (Factoría)**: Fachada única y unificada que provee métodos factoría de inicialización (`createPassphraseManager()`, `createSecurePreferences()`, `createBiometricAuthManager()`, `createIntegrityManager()`, etc.) y herramientas de validación de integridad del hardware.

### Características
- **Clave maestra respaldada por hardware**: Integración transparente con Android Keystore (TEE / StrongBox) mediante `AES-256 GCM`.
- **`PassphraseManagerImpl`**: Genera y persiste frases de contraseña con alta entropía cifradas en disco. Descarta automáticamente los bytes nulos (`0x00`), asegurando compatibilidad directa con motores de base de datos nativos como SQLCipher.
- **`SecurePreferencesImpl`**: Almacenamiento clave-valor cifrado con tipado estricto mediante `EncryptedSharedPreferences` (`AES-256 SIV` para claves y `AES-256 GCM` para valores), con serialización JSON integrada para modelos personalizados con Gson.
- **`SecureFileStorageImpl`**: Operaciones seguras sobre ficheros ejecutadas fuera del hilo principal (`Dispatchers.IO`) mediante corrutinas de Kotlin.
- **Autenticación Biométrica (`api/BiometricAuthManager`)**:
    - Comprobación de disponibilidad de hardware y credenciales registradas mediante `canAuthenticate()`.
    - Despliegue de prompts biométricos nativos con soporte para credenciales del dispositivo (PIN, patrón, contraseña).
    - Autenticación vinculada a operaciones criptográficas (`authenticateWithCrypto()`) mediante `BiometricPrompt.CryptoObject` y Android Keystore.
    - Modelado tipado exhaustivo (`BiometricResult.Success`, `BiometricResult.Error`, `BiometricResult.Failed`).
- **Integridad del Dispositivo y Entorno de Ejecución (`api/IntegrityManager`)**:
    - Detección de Root: Análisis heurístico de aplicaciones de gestión de superusuario, firmas `test-keys`, puntos de montaje sospechosos y binarios `su` habituales (`/system/xbin/su`, `/sbin/su`, etc.).
    - Detección de Emuladores y Entornos Virtuales: Identificación de entornos virtualizados (AVD, Genymotion, QEMU) mediante huellas en propiedades del sistema (`Build`), hardware y canales de comunicación simulados.
    - Detección de Depuración y Hooking Dinámico: Escaneo del mapa de memoria de la app (`/proc/self/maps`) en busca de librerías de Frida/Xposed, estado de depuración en manifiesto (`debuggable`) y depuradores activos (`android.os.Debug.isDebuggerConnected()`).
    - Informes Estructurados de Amenazas: Clasificación de riesgos mediante evaluación rápida (`isDeviceCompromised()`) o informes desglosados (`getIntegrityReport()`).
- **Atestación de Hardware**: Detección de chips dedicados StrongBox Keymaster en hardware compatible (Android 9.0+ / API 28+).
- **Manejo resiliente de errores**: Implementa el patrón funcional `Result<T>` en todas sus operaciones para prevenir cierres inesperados por excepciones criptográficas.

### Instalación
Añade el módulo de la biblioteca a tu archivo `settings.gradle.kts`:

```kotlin
include(":SecurityCore")
```

Luego declara la dependencia en el `build.gradle.kts` de tu aplicación:

```kotlin
dependencies {
    implementation(project(":SecurityCore"))
    
    // Soporte de corrutinas para operaciones asíncronas de ficheros
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // Fragment KTX requerido por la actividad anfitriona de BiometricPrompt
    implementation("androidx.fragment:fragment-ktx:1.8.6")
}
```

### Guía rápida de uso

#### 1. Generar una clave segura (ej. para SQLCipher)
```kotlin
import com.github.jragcer.securitycore.storage.PassphraseManager

val passphraseManager = PassphraseManager(context)

passphraseManager.getOrCreatePassphrase(filename = "db_passphrase.bin", length = 32)
    .onSuccess { keyBytes ->
        // Pasa keyBytes directamente a SQLCipher o Realm
    }
    .onFailure { error ->
        // Manejo de errores criptográficos o de entrada/salida
    }
```

#### 2. Almacenamiento clave-valor cifrado (`SecurePreferencesImpl`)
```kotlin
import com.github.jragcer.securitycore.storage.SecurePreferences

data class UserSession(val token: String, val role: String)

SecurePreferences.create(context, fileName = "app_secure_prefs")
    .onSuccess { prefs ->
        // Tipos primitivos
        prefs.putString("auth_token", "ejemplo_jwt_token")
        prefs.putBoolean("biometric_enabled", true)
        val token = prefs.getString("auth_token")

        // Modelos de datos complejos (serializados en JSON vía Gson)
        val session = UserSession(token = "abc-123", role = "ADMIN")
        prefs.putObject("active_session", session)

        val savedSession = prefs.getObject<UserSession>("active_session")
    }
    .onFailure { error ->
        // Manejo de corrupción de Keystore o permisos de almacenamiento
    }
```

#### 3. Persistencia de ficheros cifrados (`SecureFileStorageImpl`)
```kotlin
import androidx.lifecycle.lifecycleScope
import com.github.jragcer.securitycore.storage.SecureFileStorage
import kotlinx.coroutines.launch

val fileStorage = SecureFileStorage(context)

lifecycleScope.launch {
    val fileName = "notas_confidenciales.txt"

    // Escritura cifrada
    fileStorage.writeText(fileName, "Contenido confidencial del reporte")
        .onSuccess {
            // Archivo cifrado y persistido en disco
        }
        .onFailure { error ->
            // Manejo de fallos de escritura o cifrado
        }

    // Lectura descifrada
    fileStorage.readText(fileName)
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
import com.github.jragcer.securitycore.api.BiometricResult
import com.github.jragcer.securitycore.api.BiometricStatus

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

### Pruebas (Testing)
Ejecuta la suite de pruebas unitarias o instrumentadas:

```bash
# Pruebas unitarias
./gradlew test

# Pruebas instrumentadas con Android Keystore real
./gradlew connectedAndroidTest
```

### Arquitectura y esquema de seguridad

| Componente                | Tecnología base                                                                | Esquema criptográfico                                           |
|:--------------------------|:-------------------------------------------------------------------------------|:----------------------------------------------------------------|
| **Motor de contraseñas**  | `EncryptedFile` + Android Keystore                                             | `AES-256 GCM HKDF 4KB`                                          |
| **Motor de preferencias** | `EncryptedSharedPreferences`                                                   | Claves: `AES-256 SIV`<br>Valores: `AES-256 GCM`                 |
| **Motor de ficheros**     | `EncryptedFile` + Flujos de E/S                                                | `AES-256 GCM HKDF 4KB`                                          |
| **Motor biométrico**      | AndroidX `BiometricPrompt`                                                     | Biometría respaldada por hardware / CryptoObject                |
| **Motor deintegridad**    | Detección heurística múltiple ( `su`, `/proc/self/maps`, `propiedades Build`)  | Análisis de entorno y mitigación de manipulación (Anti-Tamper)  |

### Licencia
Distribuido bajo la licencia MIT. Consulta el archivo `LICENSE` para obtener más información.

---
