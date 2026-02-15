# Keep kotlinx serialization classes used by Ktor.
-keepclassmembers class kotlinx.serialization.** { *; }
-keep class kotlinx.serialization.** { *; }

# Keep app DTOs for JSON serialization.
-keep class com.bladderdiary.app.data.remote.dto.** { *; }
