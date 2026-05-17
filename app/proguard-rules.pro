# SQLCipher — loaded via JNI, must not be obfuscated.
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-dontwarn net.sqlcipher.**
-dontwarn net.zetetic.**

# Room — entities and DAOs accessed by generated code via reflection-ish lookups.
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Kotlinx coroutines — DebugProbesKt is referenced but we strip it via packaging.
-dontwarn kotlinx.coroutines.debug.**

# Tink (used by androidx.security.crypto) references compile-only annotations.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# Kotlin metadata used by Compose lambdas / data classes — keep names of our models.
-keep class com.xpotrack.app.data.model.** { *; }
-keep class com.xpotrack.app.data.db.** { *; }
