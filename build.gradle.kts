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

   fun push() {
      exec {
         commandLine("git", "push")
      }
   }

   fun pull() {
      exec {
         commandLine("git", "pull")
      }
   }

   fun switch(branch: String) {
      exec {
         commandLine("git", "switch", branch)
      }
   }

   fun commitVersion() {
      versionFile.writeText(version.toString())
      exec {
         commandLine("git", "commit", "-a", "-m \"$version\"")
      }
   }

   fun changeSuffix(newSuffix: String) {
      val versionParts = version.toString().split("-", limit = 2)
      version = "${versionParts[1]}${if (newSuffix.isEmpty()) "" else "-$newSuffix"}"
   }

   fun bumpRelease() {
      val versionParts = version.toString().split("-", limit = 2)
      if (versionParts.size == 2 && versionParts[1] == "rc") {
         val numbers = versionParts[0].split(".")
         version = "${numbers[0]}.${numbers[1].toInt() + 1}.${numbers[2]}-SNAPSHOT"
         commitVersion()
         push()
      }
   }

   fun bumpHotfix() {
      val versionParts = version.toString().split("-", limit = 2)
      val numbers = versionParts[0].split(".")
      version = "${numbers[0]}.${numbers[1]}.${numbers[2].toInt() + 1}"
      commitVersion()
   }

   register("featureStart") {
      doLast {
         val branch = project.properties["branch"] ?: "new"
         switch("dev")
         pull()
         exec {
            commandLine("sh", "-c", "\"git-flow feature start $branch\"")
         }
      }
   }

   register("featureFinish") {
      doLast {
         val branch = project.properties["branch"] ?: "new"
         switch("dev")
         pull()
         bumpRelease()
         exec {
            commandLine("sh", "-c", "\"git-flow feature finish -rkS $branch\"")
         }
      }
   }

   register("releaseStart") {
      doLast {
         switch("dev")
         pull()
         changeSuffix("")
         commitVersion()
         push()
         exec {
            commandLine("sh", "-c", "\"git-flow release start $version\"")
         }
      }
   }

   // assumes one release branch at a time. switch to branch when running.
   register("releaseFinish") {
      doLast {
         changeSuffix("")
         commitVersion()
         exec {
            commandLine("sh", "-c", "\"git-flow release finish -pkS -m $version '$version-rc'\"")
         }
      }
   }

   register("hotfixStart") {
      doLast {
         val branch = project.properties["branch"] ?: "new"
         switch("master")
         pull()
         exec {
            commandLine("sh", "-c", "\"git-flow hotfix start $branch\"")
         }
         changeSuffix("-hotfix")
         commitVersion()
      }
   }

   register("hotfixFinish") {
      doLast {
         val branch = project.properties["branch"] ?: "new"
         switch("master")
         pull()
         switch("hotfix/$branch")
         bumpHotfix()
         commitVersion()
         exec {
            commandLine("sh", "-c", "\"git-flow hotfix finish -p -m $version $branch\"")
         }
      }
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

   register<Exec>("composeDown") {
      workingDir("./docker/")
      commandLine(
         "docker-compose",
         "down",
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