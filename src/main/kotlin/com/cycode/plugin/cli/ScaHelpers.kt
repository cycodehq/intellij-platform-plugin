package com.cycode.plugin.cli

// keep in lowercase.
// source: https://github.com/cycodehq/cycode-cli/blob/ec8333707ab2590518fd0f36454c8636ccbf1061/cycode/cli/consts.py#L50-L82
private val SCA_CONFIGURATION_SCAN_SUPPORTED_FILES: List<String> = listOf(
    "cargo.lock",
    "cargo.toml",
    "composer.json",
    "composer.lock",
    "go.sum",
    "go.mod",
    // "gopkg.toml", // FIXME(MarshalX): missed in CLI?
    "gopkg.lock",
    "pom.xml",
    "build.gradle",
    "gradle.lockfile",
    "build.gradle.kts",
    "package.json",
    "package-lock.json",
    "yarn.lock",
    "npm-shrinkwrap.json",
    "packages.config",
    "project.assets.json",
    "packages.lock.json",
    "nuget.config",
    ".csproj",
    "gemfile",
    "gemfile.lock",
    "build.sbt",
    "build.scala",
    "build.sbt.lock",
    "pyproject.toml",
    "poetry.lock",
    "pipfile",
    "pipfile.lock",
    "requirements.txt",
    "setup.py",
    "mix.exs",
    "mix.lock",
)

private val SCA_CONFIGURATION_SCAN_LOCK_FILE_TO_PACKAGE_FILE: Map<String, String> = mapOf(
    "cargo.lock" to "cargo.toml",
    "composer.lock" to "composer.json",
    "go.sum" to "go.mod",
    "gopkg.lock" to "gopkg.toml",
    "gradle.lockfile" to "build.gradle",
    "package-lock.json" to "package.json",
    "yarn.lock" to "package.json",
    "packages.lock.json" to "nuget.config",
    "gemfile.lock" to "gemfile",
    "build.sbt.lock" to "build.sbt", // and build.scala?
    "poetry.lock" to "pyproject.toml",
    "pipfile.lock" to "pipfile",
    "mix.lock" to "mix.exs",
)

private val SCA_CONFIGURATION_SCAN_SUPPORTED_LOCK_FILES: List<String> =
    SCA_CONFIGURATION_SCAN_LOCK_FILE_TO_PACKAGE_FILE.keys.toList()

fun isSupportedPackageFile(filename: String): Boolean {
    val lowercaseFilename = filename.toLowerCase()
    SCA_CONFIGURATION_SCAN_SUPPORTED_FILES.forEach {
        if (lowercaseFilename.endsWith(it)) {
            return true
        }
    }

    return false
}

fun isSupportedLockFile(filename: String): Boolean {
    val lowercaseFilename = filename.toLowerCase()
    SCA_CONFIGURATION_SCAN_SUPPORTED_LOCK_FILES.forEach {
        if (lowercaseFilename.endsWith(it)) {
            return true
        }
    }

    return false
}

fun getPackageFileForLockFile(filename: String): String {
    val lowercaseFilename = filename.toLowerCase()
    return SCA_CONFIGURATION_SCAN_LOCK_FILE_TO_PACKAGE_FILE.getOrDefault(lowercaseFilename, "package")
}
