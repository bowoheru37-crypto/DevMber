# ==============================================================================
# HIGHLY OPTIMIZED PROGUARD / R8 RULES FOR LOW-ENTRY ANDROID DEVICES (itel A70)
# Maximum Code Shrinking, Dead Code Stripping & Aggressive Optimization
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. GENERAL COMPILER OPTIMIZATIONS & CLASS COMPACTION
# ------------------------------------------------------------------------------
-optimizationpasses 5
-allowaccessmodification
-mergeinterfacesaggressively
-overloadaggressively
-repackageclasses 'o'
-dontusemixedcaseclassnames
-skipnonpubliclibraryclasses
-dontpreverify
-verbose

# Strip logging & assertions in release builds to reduce APK size & execution overhead
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Line Numbers and Source File for crash reporting diagnostics
-keepattributes SourceFile,LineNumberTable,InnerClasses,EnclosingMethod,Signature,Exceptions,*Annotation*

# ------------------------------------------------------------------------------
# 2. KOTLIN & COROUTINES RUNTIME INTEGRITY
# ------------------------------------------------------------------------------
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    <fields>;
}

-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ------------------------------------------------------------------------------
# 3. JETPACK COMPOSE, LIFECYCLE & VIEWMODEL
# ------------------------------------------------------------------------------
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

-keepclassmembers class androidx.lifecycle.ViewModelProvider$Factory {
    public <methods>;
}

-keepclassmembers class androidx.lifecycle.ViewModelStoreOwner {
    public <methods>;
}

# ------------------------------------------------------------------------------
# 4. ROOM DATABASE & LOCAL SQLITE PERSISTENCE
# ------------------------------------------------------------------------------
-dontwarn androidx.room.paging.**

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }

-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

-keep class com.example.data.local.** { *; }
-keepclassmembers class com.example.data.local.** { *; }

# ------------------------------------------------------------------------------
# 5. MOSHI JSON SERIALIZATION & REMOTE AI DATA MODELS
# ------------------------------------------------------------------------------
-dontwarn com.squareup.moshi.**

-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}

-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class com.squareup.moshi.** { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }

-keepclassmembers class * {
    @com.squareup.moshi.FromJson *;
    @com.squareup.moshi.ToJson *;
}

# Preserve generated Moshi adapters
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}

-keep class com.example.data.remote.** { *; }
-keepclassmembers class com.example.data.remote.** { *; }

# ------------------------------------------------------------------------------
# 6. OKHTTP, RETROFIT & NETWORK ENGINES
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

-keepclassmembers class okhttp3.** {
    <fields>;
    <methods>;
}

# ------------------------------------------------------------------------------
# 7. GOOGLE CREDENTIAL MANAGER, IDENTITY & FIREBASE
# ------------------------------------------------------------------------------
-dontwarn com.google.firebase.**
-dontwarn androidx.credentials.**
-dontwarn com.google.android.libraries.identity.googleid.**

-keep class com.google.firebase.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ------------------------------------------------------------------------------
# 8. CUSTOM GAME ENGINE, BYTECODE VM, DSP & GRAPHICS PIPELINE
# Zero-Reflection Direct Execution Preservation
# ------------------------------------------------------------------------------
-keep class com.example.engine.** { *; }
-keepclassmembers class com.example.engine.** {
    public *;
    protected *;
}

# Preserve critical bitwise math & assembly registers
-keepclassmembers class com.example.engine.core.FastBinaryMath {
    public static *;
}

-keepclassmembers class com.example.engine.core.BytecodeVm {
    public *;
}

-keepclassmembers class com.example.engine.audio.BytebeatDspSynthesizer {
    public *;
}

-keepclassmembers class com.example.engine.graphics.ProceduralVideoRasterizer {
    public *;
}

-keepclassmembers class com.example.engine.graphics.MultimediaCompressor {
    public *;
}

