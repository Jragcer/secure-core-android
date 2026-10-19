# Security Showcase & Test App (`:app`)

Aplicación de demostración interactiva diseñada para verificar visualmente y probar en dispositivos reales los mecanismos del módulo `:security-core`.

---

## 🎯 Objetivo

Servir como banco de pruebas (*sandbox*) y entorno de referencia para desarrolladores que integren el SDK en sus aplicaciones.

Permite:
1. a
2. a

---

## ⚙️ Configuración para Pruebas

El módulo `:app` consume `:SecurityCore` localmente como dependencia de proyecto:

```kotlin
// app/build.gradle.kts
dependencies {
    implementation(project(":SecurityCore"))
}
```

---

## 🧪 Casos de Prueba Recomendados


---

## 🏃 Compilación y Ejecución

Compilar y desplegar la app en el dispositivo conectado mediante Gradle:

```bash
# Compilar variante Debug
./gradlew :app:assembleDebug

# Instalar y ejecutar en el dispositivo/emulador por defecto
./gradlew :app:installDebug
```