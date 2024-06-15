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
   id("nebula.dependency-lock") version "12.7.1"
   id("nebula.contacts") version "6.0.0"
   id("com.palantir.docker") version "0.36.0"
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

   task<JavaExec>("runDevJar") {
      mainClass.set("-jar")
      args = listOf(
         "${layout.projectDirectory}/build/libs/${getByName<BootJar>("bootJar").archiveFileName.get()}",
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

   fun generateCompose(map: Map<String, String>, templateFile: File) {
      val outputFile = file("docker-compose.yml")
      var template = templateFile.readText()
      map.forEach { (key, value) ->
         template = template.replace("\${$key}", value)
      }
      Files.write(outputFile.toPath(), template.toByteArray())
   }

   task("loadTokens") {
      val tokenFile = file("tokens.txt")
      tokenFile.forEachLine { line ->
         val keyValue = line.split('=')
         project.ext.set(keyValue[0].trim(), keyValue[1].trim())
      }
   }

   task("generateDevCompose") {
      val templateFile = file("compose-template-dev.yml")
      val versionMap = mapOf(
         "caddy-version" to "2.8.4-alpine",
      )
      generateCompose(versionMap, templateFile)
   }

   task("generateProdCompose") {
      dependsOn("loadTokens")
      val templateFile = file("compose-template-prod.yml")
      val versionMap = mapOf(
         "cloudflared-version" to "2024.6.0",
         "cloudflared-token" to "${project.ext.get("cloudflared-token")}"
      )
      generateCompose(versionMap, templateFile)
   }

   task<Exec>("composeUp") {
      commandLine(
         "docker-compose",
         "up",
         "-d"
      )
   }

   fun isContainerRunning(containerName: String): Boolean {
      val output = ByteArrayOutputStream()
      exec {
         standardOutput = output
         commandLine("docker", "container", "inspect", "-f", "'{{.State.Running}}'", containerName)
      }
      return output.toString().trim() == "'true'"
   }

   fun waitUntilRunning(containerName: String) {
      var running = false
      val delay = 1000L
      while (!running) {
         running = isContainerRunning(containerName)
         Thread.sleep(delay)
      }
   }

   fun runTunnel() {
      val command = listOf(
         "docker", "exec", "--user=0", "app-tunnel-1",
         "cloudflared", "tunnel", "--config", "/etc/cloudflared/conf.yml",
         "run", "app-tunnel"
      )
      val process = ProcessBuilder(command).start()
      process.waitFor(5L, TimeUnit.SECONDS)
      process.destroyForcibly()
      println("Tunnel Started!")
   }

   task("composeDevUp") {
      dependsOn(getByName("generateDevCompose"))
      dependsOn(getByName("composeUp"))
   }
   task("composeProdUp") {
      dependsOn(getByName("generateProdCompose"))
      dependsOn(getByName("composeUp"))
      doLast {
         waitUntilRunning("app-tunnel-1")
         runTunnel()
      }
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