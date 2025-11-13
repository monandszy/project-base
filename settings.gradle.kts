plugins {
  // Apply the foojay-resolver plugin to allow automatic download of JDKs
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "spring-projects"

// Loop through all subdirectories in the modules directory
val modulesDir = file("_modules")
if (modulesDir.exists() && modulesDir.isDirectory) {
  modulesDir.listFiles()?.filter { it.isDirectory }?.forEach { moduleDir ->
    include(":${moduleDir.name}")
    project(":${moduleDir.name}").projectDir = moduleDir
  }
}