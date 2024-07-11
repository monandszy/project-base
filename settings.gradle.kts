plugins {
   // Apply the foojay-resolver plugin to allow automatic download of JDKs
   id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
gradle.projectsLoaded {
   rootProject {
      version = File("project.version").readText().trim()
   }
}

rootProject.name = "project-base"
include("app")