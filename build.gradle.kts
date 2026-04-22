plugins {
    id("com.diffplug.spotless") version "8.4.0"
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

spotless {
    kotlin {
        target("app/src/*/java/**/*.kt")
        targetExclude("**/build/**")
        ktlint("1.8.0")
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:function-naming"
        }
    }

    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }
}
