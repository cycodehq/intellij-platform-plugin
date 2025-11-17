import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
    alias(libs.plugins.sentry)
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

// Configure project's dependencies
repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(libs.annotations)
    implementation(libs.jackson)
    implementation(libs.flexmark)

    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("com.intellij.java")
    }
}

// Set the JVM language level used to build the project. We are using Java 17 for 2022.2+.
kotlin {
    jvmToolchain(17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

intellijPlatform {
    // required for Auto-Reload development mode
    buildSearchableOptions = false
    projectName = project.name

    pluginConfiguration {
        name = properties("pluginName").get()
        version = properties("pluginVersion").get()

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }

        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    publishing {
        token = environment("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(
            properties("pluginVersion").map { pluginVersion: String ->
                val channel = pluginVersion.substringAfter('-', "default").substringBefore('.')
                listOf<String>(channel)
            }
        )
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    pluginVerification {
        ides {
            ide("IC-2025.2")
            // test on available 2025.3 version
            ide("IU-253.17525.95")
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
koverReport {
    defaults {
        xml {
            onCheck = true
        }
    }
}

// Configure Sentry
sentry {
    includeDependenciesReport = false

    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "cycode"
    projectName = "intellij-platform-plugin"
    authToken = environment("SENTRY_AUTH_TOKEN")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    runIde {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Didea.log.debug.categories=com.cycode.plugin"
            )
        }
    }
}

val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
  task {
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf(
        "-Drobot-server.port=8082",
        "-Dide.mac.message.dialogs.as.sheets=false",
        "-Djb.privacy.policy.text=<!--999.999-->",
        "-Djb.consents.confirmation.enabled=false",
      )
    }
  }

  plugins {
    robotServerPlugin()
  }
}

tasks.named("publishPlugin") {
    dependsOn("patchChangelog")
    dependsOn("sentryUploadSourceBundleJava")
}