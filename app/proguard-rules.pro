# Well+ Release R8 / ProGuard Hardening Rules

# Preserve @Keep annotated Firestore data models and enums for safe reflection deserialization
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep class com.example.data.model.** { *; }

# Keep Firebase Firestore PropertyName annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.DocumentId <fields>;
    @com.google.firebase.firestore.ServerTimestamp <fields>;
}

# Strip verbose, debug, and info log calls in release builds to prevent any health data from leaking to logcat
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
