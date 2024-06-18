import nebula.plugin.contacts.Contact
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.io.ByteArrayOutputStream
import java.nio.file.Files

plugins {
   application
   pmd
   jacoco
   checkstyle
   id("org.springframework.boot") version "3.2.2"
   id("com.palantir.docker") version "0.36.0"
   id("io.spring.dependency-management") version "1.1.5"
   id("nebula.dependency-lock") version "12.7.1"
}

group = "code"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
application.mainClass = "code.App"

repositories {
   mavenCentral()
}

dependencies {
   implementation("org.springframework.boot:spring-boot-starter-web")
   implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

   compileOnly("org.projectlombok:lombok:1.18.32")
   annotationProcessor("org.projectlombok:lombok:1.18.32")

   testImplementation(libs.junit.jupiter)
   testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations {
   all {
      resolutionStrategy.activateDependencyLocking()
   }
}
tasks {
   register("reLock") {
/*
diffLock - Diff existing lock and generated lock file
generateLock - Create a lock file in build/<configured name>
migrateLockeDepsToCoreLocks - Migrates Nebula-locked dependencies to use core Gradle locks
migrateToCoreLocks - Migrates all dependencies to use core Gradle locks
saveLock - Move the generated lock file into the project directory
updateLock - Apply updates to a preexisting lock file and write to build/<specified name>
gradlew :app:dependencies --write-locks
*/
   }

   bootJar {
      archiveFileName = "${project.name}-${version}.${archiveExtension.get()}"
   }
   jar {
      enabled = false
   }

   register<Exec>("extractLayers") {
      dependsOn("bootJar")
      workingDir = projectDir
      commandLine(
         "java",
         "-Djarmode=layertools",
         "-jar", "build/libs/${getByName<BootJar>("bootJar").archiveFileName.get()}",
         "extract",
         "--destination", "build/extracted"
      )
   }
   docker {
      dependsOn(getByName("extractLayers"))
      name = "${rootProject.name}/${project.name}"
      tag("monand", "latest")
      copySpec.from("${layout.projectDirectory}/build/extracted").into("extracted")
      buildArgs(mapOf("EXTRACTED" to "extracted"))
      setDockerfile(file("Dockerfile"))
   }

   test {
      useJUnitPlatform()
      testLogging {
         events("passed", "skipped", "failed")
      }
   }

   pmd {
      toolVersion = "7.0.0"
      isConsoleOutput = false
      isIgnoreFailures = true
      rulesMinimumPriority = 5
      ruleSets = listOf("category/java/errorprone.xml", "category/java/bestpractices.xml")
      pmdMain {
         exclude(
         )
      }
      pmdTest {
         exclude(
         )
      }
   }
   check {
      dependsOn(pmdMain)
      dependsOn(pmdTest)
      dependsOn(jacocoTestReport)
      dependsOn(checkstyleMain)
      dependsOn(checkstyleTest)
   }

   checkstyle {
      toolVersion = "8.42"
      isIgnoreFailures = true
      isShowViolations = false
      configFile = file("config/checkstyle.xml")
      checkstyleMain {
         source("src/main/java")
         classpath = project.files()
      }
      checkstyleTest {
         source("src/test/java")
         classpath = project.files()
      }
   }

   jacoco {
      toolVersion = "0.8.11"
      jacocoTestReport {
         reports {
            xml.required = false
            csv.required = false
            html.outputLocation = layout.buildDirectory.dir("reports/jacoco")
         }
         doLast {
            val reportPath = layout.buildDirectory.file("reports/jacoco/index.html").get().asFile
            println("Jacoco report: file://${reportPath.toURI().path}")
         }
         classDirectories.setFrom(
            files(classDirectories.files.map {
               fileTree(it) {
                  exclude(
                  )
               }
            })
         )
         dependsOn(test)
      }
   }

   javadoc {
      setDestinationDir(file(layout.buildDirectory.dir("docs")))
      options.encoding = "UTF-8"
   }
   compileJava {
      options.encoding = "UTF-8"
   }
   compileTestJava {
      options.encoding = "UTF-8"
   }
}