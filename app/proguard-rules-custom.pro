# Keep Room entities and DAOs
-keep class com.cybersaad.hackstreak.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep methods annotated with @JavascriptInterface so the bridge is not stripped
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep annotations
-keepattributes *Annotation*

# Keep kotlin metadata
-keepclassmembers class kotlin.Metadata { *; }
