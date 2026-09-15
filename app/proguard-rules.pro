# Keep Agora RTC SDK classes (required — obfuscating breaks the native bridge)
-keep class io.agora.** { *; }
-dontwarn io.agora.**
