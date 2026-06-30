# ProGuard rules for Media Saver

-keep class com.example.savemedia.services.** { *; }
-keep class com.example.savemedia.utils.AppLogger { *; }
-keep class com.example.savemedia.models.** { *; }
-keep class com.example.savemedia.domain.** { *; }

-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

-keepclassmembers class * {
    @dagger.hilt.* <methods>;
}

-assumenosideeffects class com.example.savemedia.utils.AppLogger {
    public void d(...);
}
