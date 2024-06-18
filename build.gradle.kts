import nebula.plugin.contacts.Contact
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.*

plugins {
   id("nebula.contacts") version "6.0.0"
}

contacts {
   addPerson("monandszy@tuta.io", delegateClosureOf<Contact> {
      moniker = "Szymon Andrzejewski"
      roles("developer", "maintainer")
   })
}
subprojects {
   version = rootProject.version
}

val versionFile: File by project.extra { file("project.version") }

tasks {
   fun bumpVersion(component: String, newSuffix: String? = null): String {
      val versionString = versionFile.readText().trim()

      val versionParts = versionString.split("-", limit = 2)
      val suffix =
         if (Objects.nonNull(newSuffix)) newSuffix
         else if (versionParts.size > 1) "-" + versionParts[1]
         else ""

      val versionNumbers = versionParts[0].split(".")
      val major = versionNumbers[0].toInt()
      var minor = versionNumbers[1].toInt()
      var patch = versionNumbers[2].toInt()
      when (component.lowercase()) {
         "minor" -> {
            minor += 1
            patch = 0
         }

         "patch" -> {
            patch += 1
         }

         "" -> {

         }
      }
      return "$major.$minor.$patch$suffix"
   }

   register("releaseStart") {
      val releaseVersion = bumpVersion("", "")
      val output = ByteArrayOutputStream()
      exec {
         commandLine("git", "diff", "--name-only")
         standardOutput = output
      }
      println(output.size())
//      if (output.size() != 0) {
//         exec {
//            commandLine("git", "commit", "-a", "-m \"release-$releaseVersion\"")
//         }
//      }
//      exec {
//         workingDir = rootDir;
//         commandLine("sh", "-c", "\"git-flow release start $releaseVersion\"")
//      }
      doLast {
         versionFile.writeText(releaseVersion)
      }
   }

   register("releaseFinish") {

//      val releaseVersion = bumpVersion("", "")    // unsnap version
      // run git push release (preserve it)
      // run git flow finish
      // git flow release finish '0.1.0'
      // push master
      bumpVersion("minor", "-SNAPSHOT")
   }

   register("hotfixStart") {
      bumpVersion("patch", "")
      // adjust gradle version for docker versioning separation
   }

   register("hotfixFinish") {
      // push branch for preservation
   }

   register("printVersion") {
      doLast {
         println("Project version is ${project.version}")
      }
   }

   fun generateCompose(map: Map<String, String>, templateFile: File) {
      val outputFile = file("docker/docker-compose.yml")
      var template = templateFile.readText()
      map.forEach { (key, value) ->
         template = template.replace("\${$key}", value)
      }
      Files.write(outputFile.toPath(), template.toByteArray())
   }

   fun loadFile(file: File): HashMap<String, String> {
      val variables = HashMap<String, String>()
      file.forEachLine { line ->
         if (!line.startsWith("#")) {
            val keyValue = line.split('=')
            variables[keyValue[0].trim()] = keyValue[1].trim()
         }
      }
      return variables
   }
   register("generateDevCompose") {
      val versionMap = loadFile(file("docker/versions"))
      val templateFile = file("docker/compose-template-dev.yml")
      generateCompose(versionMap, templateFile)
   }

   register("generateProdCompose") {
      val versionMap = loadFile(file("docker/versions"))
      val tokenMap = loadFile(file("docker/tokens"))
      val templateFile = file("docker/compose-template-prod.yml")
      generateCompose(versionMap + tokenMap, templateFile)
   }


   register<Exec>("composeUp") {
      workingDir("./docker/")
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
      var running: Boolean
      val delay = 3000L
      while (true) {
         running = isContainerRunning(containerName)
         if (running) return
         Thread.sleep(delay)
      }
   }

   fun runTunnel() {
      val command = listOf(
         "docker", "exec", "--user=0", "docker-tunnel-1",
         "cloudflared", "tunnel", "--config", "/etc/cloudflared/conf.yml",
         "run", "app-tunnel"
      )
      val process = ProcessBuilder(command).start()
      process.waitFor(5L, TimeUnit.SECONDS)
      process.destroyForcibly()
      println("Tunnel Started!")
   }

   register("composeDevUp") {
      dependsOn(getByName("generateDevCompose"))
      dependsOn(getByName("composeUp"))
   }
   register("composeProdUp") {
      dependsOn(getByName("generateProdCompose"))
      dependsOn(getByName("composeUp"))
      doLast {
         waitUntilRunning("docker-tunnel-1")
         runTunnel()
      }
   }
}