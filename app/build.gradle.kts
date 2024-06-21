import nebula.plugin.contacts.Contact
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.io.ByteArrayOutputStream
import java.nio.file.Files

plugins {
   application
   pmd
   jacoco
   checkstyle
   alias(libs.plugins.spring.boot)
   alias(libs.plugins.spring.dependency)
}

group = "code"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
application.mainClass = "code.App"

repositories {
   mavenCentral()
}

dependencies {
   implementation(libs.bundles.spring)
   compileOnly(libs.lombok)
   annotationProcessor(libs.lombok)
   testImplementation(libs.junit.jupiter)
   testRuntimeOnly(libs.junit.platform)
}
java {
   @Suppress("UnstableApiUsage")
   consistentResolution {
      useCompileClasspathVersions()
   }
}

tasks {

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
   register("docker") {
      dependsOn(getByName("extractLayers"))
      doLast {
         exec {
            commandLine(
               "docker",
               "build",
               "--build-arg", "EXTRACTED=build/extracted",
               "-t", "${rootProject.name}/${project.name}:$version",
               "-q",
               "."
            )
         }
      }
   }

   test {
      useJUnitPlatform()
      testLogging {
         events("passed", "skipped", "failed")
      }
   }

   pmd {
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