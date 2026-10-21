# AndroidX Leanback reflection rules
-keep class androidx.leanback.** { *; }
-keep interface androidx.leanback.** { *; }
-keep class androidx.tvprovider.** { *; }

# Keep domain models
-keep class com.unix14.minimaltv.domain.model.** { *; }

# Optimize logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
