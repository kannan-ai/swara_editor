# ======================================================================================
# UNIVERSAL APPLICATION R8 / PROGUARD PRODUCTION RULES
# ======================================================================================

# 1. Room Persistence & SQLite Components
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
    @androidx.room.ProvidedTypeConverter *;
}
-dontwarn androidx.room.**

# 2. Universal Data Transfer Objects (DTO) & Serialization Models
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.squareup.moshi.Json <fields>;
    @kotlinx.serialization.SerialName <fields>;
}
# Universal wildcard matching any package root (e.g., com.example, com.swara, etc.)
-keep class **.data.local.dto.** { *; }
-keep class **.data.local.entity.** { *; }
-keep class **.data.local.relation.** { *; }
-keep class **.ui.model.** { *; }
-keep class com.example.swara_editor.videoeditor.models.** { *; }

# 3. Media3 & ExoPlayer Hardware Components (For Media/Video Modules)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.effect.** { *; }
-keep class androidx.media3.transformer.** { *; }
-keep class com.example.swara_editor.videoeditor.utils.** { *; }
-dontwarn androidx.media3.**

# 4. Coil & Coroutines Background Dispatchers
-keep class coil.** { *; }
-dontwarn coil.**
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepclassmembers class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# 5. Native JNI & WebRTC Wrappers (For Voice/P2P/AI Engines)
-keepclasseswithmembernames class * {
    native <methods>;
}

# 6. Jetpack Compose Animation & Layout Primitives
-keepclassmembers class androidx.compose.animation.core.** { *; }
-dontwarn androidx.compose.**
