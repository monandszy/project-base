
import nebula.plugin.contacts.Contact
import java.io.ByteArrayOutputStream
import java.nio.file.Files

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
         commandLine("git", "push", "-q")
      }
   }

   fun pull() {
      try {
         exec {
            commandLine("git", "pull", "-q")
            isIgnoreExitValue = true
         }
      } catch (e: Exception) {
         logger.warn("Git pull failed: ${e.message}")
      }
   }

   fun switch(branch: String) {
      exec {
         commandLine("git", "switch", "-q", branch)
      }
   }

   fun commitVersion() {
      versionFile.writeText(version.toString())
      val output = ByteArrayOutputStream()
      exec {
         commandLine("git", "diff", "--name-only")
         standardOutput = output
      }
      if (output.size() != 0) {
         exec {
            commandLine("git", "commit", "-a", "-m \"$version\"")
         }
      }
   }

   fun changeSuffix(newSuffix: String) {
      val versionParts = version.toString().split("-", limit = 2)
      version = "${versionParts[0]}${if (newSuffix.isEmpty()) "" else "-$newSuffix"}"
      commitVersion()
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
         switch("dev")
         pull()
         val branch = project.properties["branch"] ?: throw GradleException("-Pbranch=name not provided")
         exec {
            commandLine("sh", "-c", "\"git-flow feature start $branch\"")
         }
      }
   }

   register("featureFinish") {
      doLast {
         val branch = project.properties["branch"] ?: throw GradleException("-Pbranch=name not provided")
         switch("feature/$branch")
         switch("dev")
         pull()
         bumpRelease()
         exec {
            commandLine("sh", "-c", "\"git-flow feature finish -kS $branch\"")
         }
      }
   }

   register("releaseStart") {
      doLast {
         switch("dev")
         changeSuffix("rc")
         push()
         exec {
            commandLine("sh", "-c", "\"git-flow release start ${version.toString().split("-")[0]}\"")
         }
      }
   }

   // assumes one release branch at a time. switch to branch when running.
   // if fails because of merge conflicts to dev, accept master's version, commit, and run again (version should bump)
   register("releaseFinish") {
      doLast {
         changeSuffix("")
         exec {
            commandLine("sh", "-c", "\"git-flow release finish -pS -m $version '$version'\"")
         }
         bumpRelease()
      }
   }

   register("hotfixStart") {
      doLast {
         switch("master")
         val branch = project.properties["branch"] ?: throw GradleException("-Pbranch=name not provided")
         pull()
         exec {
            commandLine("sh", "-c", "\"git-flow hotfix start $branch\"")
         }
         changeSuffix("hotfix")
      }
   }

   register("hotfixFinish") {
      doLast {
         switch("master")
         val branch = project.properties["branch"] ?: throw GradleException("-Pbranch=name not provided")
         pull()
         switch("hotfix/$branch")
         bumpHotfix()
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

   fun generateCompose(map: Map<String, String>, templateFile: File, outputFile: File) {
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
      variables["app-version"] = "$version"
      return variables
   }
   register("generateDevCompose") {
      doLast {
         val versionMap = loadFile(file("docker/versions"))
         generateCompose(versionMap, file("docker/compose-template-dev.yml"), file("docker/compose-dev.yml"))
         generateCompose(versionMap, file("docker/compose-template-observability.yml"), file("docker/compose-observability.yml"))
      }
   }
   register("generateProdCompose") {
      doLast {
         val versionMap = loadFile(file("docker/versions"))
         generateCompose(versionMap, file("docker/compose-template-prod.yml"), file("docker/compose-prod.yml"))
         generateCompose(versionMap, file("docker/compose-template-observability.yml"), file("docker/compose-observability.yml"))
      }
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
      exec {
         workingDir("./docker/tunnel/")
         commandLine(
            "bash", "run_tunnel.sh", "&"
         )
      }
   }

   fun composeUp(projectName: String) {
      exec {
         workingDir("./docker/")
         commandLine(
            "docker", "compose",
            "-p", projectName,
            "-f", "compose-$projectName.yml",
            "up",
            "-d",
            "--remove-orphans",
         )
      }
   }

   fun restartBackend(projectName: String) {
      exec {
         workingDir("./docker/")
         commandLine(
            "docker", "compose",
            "-p", projectName,
            "-f", "compose-$projectName.yml",
            "up",
            "-d",
            "--force-recreate", "backend"
         )
      }
   }

   fun composeObservabilityUp() {
      exec {
         workingDir("./docker/")
         commandLine(
            "docker", "compose",
            "-p", "observability",
            "-f", "compose-observability.yml",
            "up",
            "-d",
            "--no-recreate"
         )
      }
   }

   register("stopDevBackend") {
      doLast {
         exec {
            commandLine("docker", "stop", "dev-backend-1")
         }
      }
   }

   register("restartDevBackend") {
      dependsOn("app:docker")
      doLast {
         restartBackend("dev")
      }
   }
   register("restartProdBackend") {
      dependsOn("app:docker")
      doLast {
         restartBackend("prod")
      }
   }

   register("composeDevUp") {
      dependsOn("app:docker")
      dependsOn("generateDevCompose")
      doLast {
         composeObservabilityUp()
         waitUntilRunning("observability-grafana-1")
         composeUp("dev")
      }
   }

   register("composeProdUp") {
      dependsOn("app:docker")
      dependsOn("generateProdCompose")
      doLast {
         composeObservabilityUp()
         waitUntilRunning("observability-grafana-1")
         composeUp("prod")
         waitUntilRunning("prod-tunnel-1")
         runTunnel()
      }
   }

   register("composeDevDown") {
      doLast {
         exec {
            commandLine("docker", "compose", "-p", "dev", "down")
         }
      }
   }
   register("composeProdDown") {
      doLast {
         exec {
            commandLine("docker", "compose", "-p", "prod", "down")
         }
      }
   }
   register("composeObservabilityDown") {
      doLast {
         exec {
            commandLine("docker", "compose", "-p", "observability", "down")
         }
      }
   }
}