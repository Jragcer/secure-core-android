# Mantener clases públicas de SecureCore
-keep class com.github.jragcer.securitycore.storage.** { *; }

# Atributos de reflexión y anotaciones para Gson
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# AndroidX Crypto y Tink
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**