import org.gradle.api.tasks.bundling.Jar
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  // Java support
  id("java")
  // Kotlin support
  id("org.jetbrains.kotlin.jvm") version "2.4.10"
  // IntelliJ Platform Gradle Plugin
  id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "org.lso"
version = "2026.2.0"
val platformVersion = providers.gradleProperty("platformVersion").getOrElse("2024.2")

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(25))
  }
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
  }
}

// Configure project's dependencies
repositories {
  mavenCentral()


  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    webstorm(platformVersion)
    bundledPlugin("JavaScript")
    testFramework(TestFrameworkType.Platform)
  }
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.opentest4j:opentest4j:1.3.0")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
  pluginConfiguration {
    name.set("LogIt")
    version.set(project.version.toString())
    ideaVersion {
      sinceBuild.set("242")
      untilBuild.set(provider { null })
    }
  }
  pluginVerification {
    failureLevel = VerifyPluginTask.FailureLevel.ALL
    verificationReportsDirectory = file("build/reports/pluginVerifier")
    verificationReportsFormats = VerifyPluginTask.VerificationReportsFormats.ALL
    teamCityOutputFormat = false
    subsystemsToCheck = VerifyPluginTask.Subsystems.ALL
    ides {
      create(IntelliJPlatformType.WebStorm, "2024.2")
      create(IntelliJPlatformType.WebStorm, "2026.2")
      select {
        types = listOf(IntelliJPlatformType.WebStorm)
        channels = listOf(ProductRelease.Channel.RELEASE, ProductRelease.Channel.EAP)
        sinceBuild = "262"
        untilBuild = "262.*"
      }
    }
  }

  publishing {
    token.set(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
  }

  tasks {
    patchPluginXml {
      changeNotes.set(
        """<br>
      v2026.2.0 - compatibility with IntelliJ Platform 2026.2<br>
      v2025.1 - compatibility with 2025.1 version<br>
      v2024.31 - remove deprecated functions<br>
      v2024.3 - compatibility with 2024.3 version<br>
      v2024.21 - compatibility with 2024.202 version<br>
      v2024.2 - compatibility with 2024.2 version<br>
      v2024.1 - compatibility with 2024.1 version<br>
      v2023.3 - compatibility with 2023.3 version<br>
      v2023.21 - compatibility with 2023.2 version<br>
      v2023.1 - compatibility with 2023 version<br>
      v2022.2 - command to delete LogIt logs from file or project<br>
      v2022.1 - add patterns to add new info in the log line<br>
      v2021.1.2 - replace a deprecated api<br>
      v2021.1 - compatibility with 2021 version and the following ones<br>
      v2020.3.11 - new icon<br>
      v2020.3.1 - adding configuration settings<br>
      v2020.3 - to WebStorm 2020.3<br>
      v2020.2 - to WebStorm 2020.2<br>
      v201.1.1 - to WebStorm 2020.<br>
      v193.3.1 - some corrections when inserting a console.log from an empty line.<br>
      v1.0 - initial release.<br>
"""
      )
    }
  }
}

tasks.named<Jar>("jar") {
  from(rootProject.file("LICENSE")) {
    into("META-INF")
  }
  from(rootProject.file("NOTICE")) {
    into("META-INF")
  }
}

tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(21)
}
