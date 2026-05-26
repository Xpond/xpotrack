# SQLCipher (net.zetetic) — JNI-loaded native lib + reflection from Room's
# SupportOpenHelperFactory. Library ships consumer rules, but pin the public
# API explicitly so a future minor bump can't silently strip it.
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.**

# Room — our @Entity / @Dao / @Database classes are looked up by name by the
# generated *_Impl. Library's consumer rules cover androidx.room internals.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Our entity / DAO / migration classes — Room's generated code references
# field names and constructors directly, so name-preserving is required.
-keep class com.xpotrack.app.data.db.** { *; }
-keep class com.xpotrack.app.data.model.** { *; }

# Kotlinx coroutines DebugProbes — referenced but stripped from the APK
# via the packaging block in build.gradle.kts.
-dontwarn kotlinx.coroutines.debug.**

# Tink (transitive via androidx.security.crypto) references compile-only
# annotations that aren't on the Android runtime classpath.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
