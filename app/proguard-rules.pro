# Keep kotlinx serialization classes used by Ktor.
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep app DTOs for JSON serialization.
-keep class com.bladderdiary.app.data.remote.dto.** { *; }

# SLF4J 바인더는 안드로이드 런타임에서 직접 사용하지 않아 R8 경고만 억제합니다.
-dontwarn org.slf4j.impl.StaticLoggerBinder
