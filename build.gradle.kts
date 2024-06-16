import nebula.plugin.release.git.opinion.Strategies
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.HashMap

plugins {
   id("com.netflix.nebula.release") version "19.0.9"
}

/*
diffLock - Diff existing lock and generated lock file
generateLock - Create a lock file in build/<configured name>
migrateLockeDepsToCoreLocks - Migrates Nebula-locked dependencies to use core Gradle locks
migrateToCoreLocks - Migrates all dependencies to use core Gradle locks
saveLock - Move the generated lock file into the project directory
updateLock - Apply updates to a preexisting lock file and write to build/<specified name>
gradlew :app:dependencies --write-locks
*/

tasks {
   register("reLock") {

   }

   release {
      remote = "origin"
      defaultVersionStrategy = Strategies.getSNAPSHOT()
   }
   nebulaRelease {
      allowReleaseFromDetached = false
      checkRemoteBranchOnRelease = true
// defaults:
//   Release: [master, HEAD, main, (release(-|/))?\d+(\.\d+)?\.x, v?\d+\.\d+\.\d+]
//   Exclude: []
//   Short:  (?:(?:bugfix|feature|hotfix|release)(?:-|/))?(.+)

//   shortenedBranchPattern = ""
//   addReleaseBranchPattern("dev")
//   addExcludeBranchPattern("")
      /*
      gradlew final -Prelease.version=0.1.0
      */
      named("final") {
         doLast {
            println("Final release created with version ${project.version}")
         }
      }
      register("printVersion") {
         doLast {
            println("Project version is ${project.version}")
         }
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