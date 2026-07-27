# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Essentials core classes
-keep class com.isaacshub.essentials.core.** { *; }

# Keep data models for serialization
-keep class com.isaacshub.essentials.data.** { *; }
-keepclassmembers class com.isaacshub.essentials.data.** { *; }

# Keep Room entities and DAOs
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.isaacshub.essentials.**$$serializer { *; }
-keepclassmembers class com.isaacshub.essentials.** {
    *** Companion;
}
-keepclasseswithmembers class com.isaacshub.essentials.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DeviceAdminReceiver
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }
-keep class com.isaacshub.essentials.admin.** { *; }

# Keep BroadcastReceivers
-keep class * extends android.content.BroadcastReceiver { *; }

# Keep Services
-keep class * extends android.app.Service { *; }

# Jetpack Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# CameraX
-dontwarn androidx.camera.**
-keep class androidx.camera.** { *; }

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# OkHttp (if used indirectly)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Security-crypto / Tink
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

