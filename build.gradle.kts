import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("org.jetbrains.changelog") version "2.2.0"
}

group = "net.odyssi"
version = "0.6.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {

    compileOnly("org.slf4j:slf4j-api:2.0.13")

    intellijPlatform {
        create("IC", "2025.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = "253.*"
        }

        changeNotes = """
      What's new in Log4JB v0.6.0:
      <ul>
        <li>Fixed duplicate log statements being inserted when "Log This Method" or "Log This Class" is invoked multiple times on the same method</li>
        <li>Improved logger declaration to detect existing SLF4J logger fields regardless of field name</li>
        <li>Hardened "Reapply Logging" to only modify statements on the actual logger field</li>
        <li>All actions now register named undo commands for clearer Edit > Undo entries</li>
        <li>Fixed "Log at this position..." action visibility and null-safety</li>
        <li>Local variable detection now includes variables declared in nested scopes</li>
        <li>Removed erroneous debug output from generic log action</li>
      </ul>
    """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}
