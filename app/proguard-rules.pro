# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# -------------------- GENERAL --------------------
# Mantieni info per stack traces leggibili
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Mantieni annotazioni
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# -------------------- KOTLIN --------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# -------------------- HILT / DAGGER --------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembers class * {
    @javax.inject.* <methods>;
}

# Hilt Generated
-keep class *_HiltModules* { *; }
-keep class *_HiltComponents* { *; }
-keep class *_ComponentTreeDeps* { *; }

# -------------------- ROOM --------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# Room - Keep domain models e entities
-keep class net.calvuz.qstore.data.local.entity.** { *; }
-keep class net.calvuz.qstore.domain.model.** { *; }

# -------------------- OPENCV --------------------
-keep class org.opencv.** { *; }
-keep class org.opencv.core.** { *; }
-keep class org.opencv.imgproc.** { *; }
-keep class org.opencv.features2d.** { *; }
-dontwarn org.opencv.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# -------------------- CAMERAX --------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# -------------------- COMPOSE --------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Compose stability
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# -------------------- DATASTORE (Preferences) --------------------
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# -------------------- APACHE POI --------------------
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.**
-dontwarn org.openxmlformats.**
-dontwarn com.microsoft.**
-dontwarn org.etsi.**
-dontwarn org.w3.**

-keep class org.apache.poi.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class com.microsoft.schemas.** { *; }

# POI richiede queste classi per XLSX
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class org.apache.poi.xssf.** { *; }
-keep class org.apache.poi.ss.** { *; }

-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference

# -------------------- JACKSON --------------------
-keep class com.fasterxml.jackson.** { *; }
-keep class com.fasterxml.jackson.databind.** { *; }
-dontwarn com.fasterxml.jackson.**

# -------------------- COIL --------------------
-keep class coil.** { *; }
-dontwarn coil.**

# -------------------- UUID --------------------
-keep class com.github.f4b6a3.uuid.** { *; }

# -------------------- ACCOMPANIST --------------------
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# -------------------- APP SPECIFIC --------------------
# Mantieni tutte le classi del tuo package principale
-keep class net.calvuz.qstore.** { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# Use Cases (se usano reflection)
-keep class net.calvuz.qstore.domain.usecase.** { *; }

# Repositories
-keep class net.calvuz.qstore.domain.repository.** { *; }
-keep class net.calvuz.qstore.data.repository.** { *; }

# -------------------- DEBUGGING --------------------
# Rimuovi log in release (opzionale ma consigliato)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    # Mantieni warn e error per crash reporting
    # public static int w(...);
    # public static int e(...);
}

# -------------------- COMMON WARNINGS TO SUPPRESS --------------------
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**