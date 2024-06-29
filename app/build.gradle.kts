import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
   application
   pmd
   jacoco
   checkstyle
   alias(libs.plugins.spring.boot)
   alias(libs.plugins.spring.management)
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
   maven {
      url = uri("https://repo.spring.io/release")
   }
}

dependencies {

   implementation(libs.bundles.spring.modulith)
   implementation(libs.bundles.observability)
//   implementation("org.springframework.boot:spring-boot-configuration-processor")

   implementation(libs.bundles.spring.web)
//   implementation("org.springframework.session:spring-session-core")
//   implementation("org.springframework.boot:spring-boot-starter-security")
//   implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
//   testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
//   testImplementation("org.springframework.security:spring-security-test")

   compileOnly(libs.lombok)
   annotationProcessor(libs.lombok)
   runtimeOnly("org.postgresql:postgresql")
   implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//   implementation("org.springframework.modulith:spring-modulith-starter-jpa")

//   implementation("org.liquibase:liquibase-core")

   testImplementation("org.springframework.boot:spring-boot-testcontainers")
   testImplementation("org.testcontainers:postgresql")
   testImplementation("org.testcontainers:junit-jupiter")
   testImplementation(libs.junit.jupiter)
   testRuntimeOnly(libs.junit.platform)
   testImplementation(libs.bundles.spring.test)

   "developmentOnly"("org.springframework.boot:spring-boot-devtools")
}

dependencyManagement {
   imports {
      mavenBom("org.springframework.modulith:spring-modulith-bom:1.2.1")
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
      dependsOn("bootJar")
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