import nebula.plugin.contacts.Contact
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
   application
   pmd
   jacoco
   checkstyle
   id("org.springframework.boot") version "3.2.2"
   id("nebula.dependency-lock") version "12.7.1"
   id("nebula.contacts") version "6.0.0"
   id("com.palantir.docker") version "0.36.0"
   id("com.palantir.docker-run") version "0.36.0"
   id ("com.palantir.docker-compose") version "0.36.0"
   id("io.spring.dependency-management") version "1.1.5"
}

group = "code"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)
application.mainClass = "code.App"

contacts {
   addPerson("monandszy@tuta.io", delegateClosureOf<Contact> {
      moniker = "Szymon Andrzejewski"
      roles("developer", "maintainer")
   })
}

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
   docker("library:caddy:2.8.4-alpine")
   }
configurations {
   all {
      resolutionStrategy.activateDependencyLocking()
   }
}
tasks {
   bootJar {
      archiveFileName = "${project.name}-${version}.${archiveExtension.get()}"
   }
   jar {
      enabled = false
   }

   task<JavaExec>("runJarDev") {
      mainClass.set("-jar")
      args = listOf(
         "${layout.buildDirectory}/libs/${getByName<BootJar>("bootJar").archiveFileName.get()}",
         "-Dspring.profiles.active=dev"
      )
   }

   task<Exec>("extractLayers") {
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
      dependsOn(getByName<Exec>("extractLayers"))
      name = "${rootProject.name}/${project.name}"
      tag("monand", "latest")
      copySpec.from("${layout.projectDirectory}/build/extracted").into("extracted")
      buildArgs(mapOf("EXTRACTED" to "extracted"))
      setDockerfile(file("Dockerfile"))
   }
   dockerRun {
      named("dockerRun") {
         dependsOn(docker)
      }
      name = "${project.name}-${version}"
      image = "${rootProject.name}/${project.name}"
      ports("8080:8080")
      arguments("-d")
      clean = true
      daemonize = false
   }
   dockerCompose {
      dockerComposeUp {
         dependsOn("docker")
      }
      setDockerComposeFile(file("docker-compose.yml"))
      setTemplate(file("compose-template.yml"))
      templateTokens = mapOf(
         Pair("m", "")
      )
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
      toolVersion = "7.0.0"
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
      toolVersion = "8.42"
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