# CLIPKU+ ProGuard rules
# Retrofit + Gson: keep model classes
-keep class com.clipku.drama.data.model.** { *; }
-keep class com.clipku.drama.data.api.** { *; }
# Retrofit interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
# Coil
-keep class coil.** { *; }
-dontwarn coil.**
# Compose: keep composable functions (R8 handles most, this is safety)
-keepattributes *Annotation*
# WorkManager: worker diinstansiasi via Class.forName — jangan obfuscate
-keep class com.clipku.drama.data.notify.EpisodeCheckWorker { *; }
# Cast framework: consumer rules bawaan AAR, tambahan safety
-dontwarn com.google.android.gms.cast.**
