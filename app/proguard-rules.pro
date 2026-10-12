# ------------------------------------------------------------------
# R8 / ProGuard rules - Catat Emas
# ------------------------------------------------------------------

# kotlinx.serialization ------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*

-keep,includedescriptorclasses class com.rizal.catatemas.**$$serializer { *; }
-keepclassmembers class com.rizal.catatemas.** {
    *** Companion;
}
-keepclasseswithmembers class com.rizal.catatemas.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class com.rizal.catatemas.** {
    static **$Companion Companion;
}
-keepnames @kotlinx.serialization.Serializable class com.rizal.catatemas.**

# OkHttp / Okio --------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Coroutines -----------------------------------------------------------
-dontwarn kotlinx.coroutines.**
