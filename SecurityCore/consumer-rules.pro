# SecureCore consumer rules

# Keep generic type info so Gson's TypeToken (used by SecurePreferences.getObject<T>()) works after R8.
-keepattributes Signature

# Tink (pulled in by androidx.security-crypto): its protobuf-lite messages are read by reflection.
# Tink may already ship this rule; it is kept here as a cheap safety net.
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**