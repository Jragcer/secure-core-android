# SecureCore

![Android CI](https://github.com/jragcer/SecuritySdkDemo/actions/workflows/build.yaml/badge.svg)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-blue.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)

A robust, enterprise-grade Android security library written in Kotlin.  
*Una biblioteca de seguridad para Android robusta y de nivel empresarial desarrollada en Kotlin.*

---

## Language / Idioma
- [English](#english)
- [Español](#español)

---

<a name="english"></a>
## English

### Overview
`SecureCore` simplifies the adoption of high-standard cryptographic persistence, hardware-backed key management, and modern secure storage patterns across Android applications.

### Features
- **Hardware-Backed Master Key**: Integrates transparently with the Android Keystore (TEE / StrongBox) using `AES-256 GCM`.
- **`PassphraseManager`**: Generates and persists high-entropy passphrases encrypted on disk. It automatically discards `0x00` (null bytes), guaranteeing direct compatibility with native database engines like SQLCipher.
- **`SecurePreferences`**: Type-safe encrypted key-value storage powered by `EncryptedSharedPreferences` (`AES-256 SIV` for keys and `AES-256 GCM` for values) with out-of-the-box JSON serialization for custom models via Gson.
- **`SecureFileStorage`**: High-performance encrypted file operations running off the main thread (`Dispatchers.IO`) through Kotlin Coroutines.
- **Resilient Error Handling**: Uses Kotlin's `Result<T>` pattern across all operations to prevent uncaught cryptographic crashes.

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
}
```

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

#### 2. Encrypted Key-Value Storage (`SecurePreferences`)
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

#### 3. Encrypted File Persistence (`SecureFileStorage`)
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

### Testing
The library includes an instrumented test suite running against real Keystore instances (`src/androidTest`):

```bash
./gradlew connectedAndroidTest
```

### Architecture & Security Overview

| Component | Underlying Technology | Cryptographic Scheme |
| :--- | :--- | :--- |
| **Passphrase Engine** | `EncryptedFile` + Android Keystore | `AES-256 GCM HKDF 4KB` |
| **Preference Engine** | `EncryptedSharedPreferences` | Keys: `AES-256 SIV`<br>Values: `AES-256 GCM` |
| **File Engine** | `EncryptedFile` + IO Streams | `AES-256 GCM HKDF 4KB` |

---

<a name="español"></a>
## Español

### Descripción general
`SecureCore` simplifica la adopción de persistencia criptográfica con altos estándares, gestión de claves respaldada por hardware y patrones modernos de almacenamiento seguro en aplicaciones Android.

### Características
- **Clave maestra respaldada por hardware**: Integración transparente con Android Keystore (TEE / StrongBox) mediante `AES-256 GCM`.
- **`PassphraseManager`**: Genera y persiste frases de contraseña con alta entropía cifradas en disco. Descarta automáticamente los bytes nulos (`0x00`), asegurando compatibilidad directa con motores de base de datos nativos como SQLCipher.
- **`SecurePreferences`**: Almacenamiento clave-valor cifrado con tipado estricto mediante `EncryptedSharedPreferences` (`AES-256 SIV` para claves y `AES-256 GCM` para valores), con serialización JSON integrada para modelos personalizados con Gson.
- **`SecureFileStorage`**: Operaciones seguras sobre ficheros ejecutadas fuera del hilo principal (`Dispatchers.IO`) mediante corrutinas de Kotlin.
- **Manejo resiliente de errores**: Implementa el patrón funcional `Result<T>` en todas sus operaciones para prevenir cierres inesperados de la aplicación por errores criptográficos.

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

#### 2. Almacenamiento clave-valor cifrado (`SecurePreferences`)
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

#### 3. Persistencia de ficheros cifrados (`SecureFileStorage`)
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

### Pruebas (Testing)
La capa de persistencia se apoya en los proveedores de Android Keystore del sistema operativo. Por ello, la suite de pruebas está configurada como instrumentada bajo `src/androidTest`:

```bash
./gradlew connectedAndroidTest
```

### Arquitectura y esquema de seguridad

| Componente | Tecnología base | Esquema criptográfico |
| :--- | :--- | :--- |
| **Motor de contraseñas** | `EncryptedFile` + Android Keystore | `AES-256 GCM HKDF 4KB` |
| **Motor de preferencias** | `EncryptedSharedPreferences` | Claves: `AES-256 SIV`<br>Valores: `AES-256 GCM` |
| **Motor de ficheros** | `EncryptedFile` + Flujos de E/S | `AES-256 GCM HKDF 4KB` |