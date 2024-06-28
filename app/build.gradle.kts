import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
   application
   pmd
   jacoco
   checkstyle
   alias(libs.plugins.spring.boot)
   alias(libs.plugins.spring.management)
//   id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "code"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
application.mainClass = "code.App"

java {
   @Suppress("UnstableApiUsage")
   consistentResolution {
      useCompileClasspathVersions()
   }
}

repositories {
   mavenCentral()
}

dependencies {
   implementation(libs.bundles.spring)
   compileOnly(libs.lombok)
   annotationProcessor(libs.lombok)
   testImplementation(libs.junit.jupiter)
   testRuntimeOnly(libs.junit.platform)
   implementation("org.springframework.boot:spring-boot-starter-actuator")
   implementation("io.micrometer:micrometer-registry-prometheus")
   implementation("org.springframework.modulith:spring-modulith-starter-core")
   testImplementation("org.springframework.boot:spring-boot-starter-test")
   testImplementation("org.springframework.modulith:spring-modulith-starter-test")
   implementation("org.springframework.modulith:spring-modulith-events-api:1.2.1")
//   runtimeOnly("org.springframework.modulith:spring-modulith-actuator:1.2.1")
   implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
}

dependencyManagement {
   imports {
      mavenBom("org.springframework.modulith:spring-modulith-bom:1.2.1")
      mavenBom("io.opentelemetry:opentelemetry-bom:1.39.0")
      mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:2.4.0-alpha")
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