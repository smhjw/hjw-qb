-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault

# Retrofit service interface: generic suspend signatures are resolved reflectively.
# Retrofit ships consumer rules, but keep our API surface explicitly so an R8
# full-mode behavior change can never strip/rename it silently.
-keep interface com.hjw.qbremote.data.QbApi { *; }

# Gson TypeToken subtypes capture generics via Signature reflection; converter-gson
# 2.11 pulls Gson 2.10.x whose jar does not yet bundle these rules.
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Gson serializes enums by constant name; renaming them would corrupt persisted
# JSON (server profiles, caches) written by earlier releases.
-keepclassmembers enum com.hjw.qbremote.data.** { *; }

# Gson-backed persisted settings and dashboard caches.
-keep class com.hjw.qbremote.data.ConnectionSettings { *; }
-keep class com.hjw.qbremote.data.ServerProfile { *; }
-keep class com.hjw.qbremote.data.ServerProfilesState { *; }
-keep class com.hjw.qbremote.data.ServerDashboardPreferences { *; }
-keep class com.hjw.qbremote.data.DashboardCacheSnapshot { *; }
-keep class com.hjw.qbremote.data.CachedDashboardServerSnapshot { *; }
-keep class com.hjw.qbremote.data.DailyUploadTrackingSnapshot { *; }
-keep class com.hjw.qbremote.data.DailyCountryUploadTrackingSnapshot { *; }
-keep class com.hjw.qbremote.data.model.CountryPeerSnapshot { *; }

# Transmission and qB DTOs parsed via Gson / Retrofit converters.
-keep class com.hjw.qbremote.data.model.** { *; }
-keep class com.hjw.qbremote.data.TransmissionSessionInfo { *; }
-keep class com.hjw.qbremote.data.TransmissionSessionStats { *; }
-keep class com.hjw.qbremote.data.TransmissionCumulativeStats { *; }
-keep class com.hjw.qbremote.data.TransmissionTorrent { *; }
-keep class com.hjw.qbremote.data.TransmissionTracker { *; }
-keep class com.hjw.qbremote.data.TransmissionTrackerStat { *; }
-keep class com.hjw.qbremote.data.TransmissionFile { *; }
-keep class com.hjw.qbremote.data.TransmissionFileStat { *; }

# Preserve serialized field names used by Gson reflection.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Strip non-error logging from release builds. The only current call site
# (MainViewModel Log.w) carries server address details that must not reach
# logcat in release. Log.e is intentionally kept for future fatal errors.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
