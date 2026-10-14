# ── Debugging ────────────────────────────────────────────────
# Keep stack-trace line numbers so release-APK crashes stay diagnosable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin / Compose essentials ──────────────────────────────
-dontwarn kotlinx.**
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Compose compiler generates stable classes that are referenced by name
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Gson — all serialized data classes live in this one package.
# Everything under com.example.un_signed.* that Gson round-trips (UpdateInfo,
# CalendarTask, FitnessSample, BpReading, GlucoseReading, UserProfile,
# AppPreferences, Habit, Subject, LectureState, … etc) must retain its fields
# and no-arg constructor path.
-keep class com.example.un_signed.** { *; }
-keepclassmembers,allowobfuscation class com.example.un_signed.** {
  <init>(...);
  <fields>;
}
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,AnnotationDefault
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# ── Health Connect — reflection-based record types.
-keep class androidx.health.connect.client.records.** { *; }
-keep class androidx.health.connect.client.aggregate.** { *; }
-keep class androidx.health.connect.client.units.** { *; }
-dontwarn androidx.health.connect.**

# ── Activity / entry points — manifest-registered classes must survive.
-keep class com.example.un_signed.SplashActivity
-keep class com.example.un_signed.ProfileSelectionActivity
-keep class com.example.un_signed.TaskReminderReceiver

# ── Reflection on package info (version lookups)
-keepclassmembers class * {
  public static <fields>;
}
