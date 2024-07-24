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
  version = File("${projectDir}/project.version").readText().trim()
}

apply(from = rootProject.file("gradle/util/misc.gradle.kts"))
apply(from = rootProject.file("gradle/util/git.gradle.kts"))

tasks {
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

  fun generateCompose(map: Map<String, String>, templateFile: File, outputFile: File) {
    var template = templateFile.readText()
    map.forEach { (key, value) ->
      template = template.replace("\${$key}", value)
    }
    Files.write(outputFile.toPath(), template.toByteArray())
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

  register("generateBaseCompose") {
    doLast {
      val versionMap = loadFile(file("docker/versions"))
      val template = "docker/template/"
      val destination = "docker/"
      generateCompose(
        versionMap,
        file("${template}compose-observability.yml"),
        file("${destination}compose-observability.yml")
      )
      generateCompose(
        versionMap,
        file("${template}compose-data.yml"),
        file("${destination}compose-data.yml")
      )
    }
  }

  fun composeUp(projectName: Any) {
    exec {
      workingDir("./docker/")
      commandLine(
        "docker", "compose",
        "-p", projectName,
        "-f", "compose-$projectName.yml",
        "up",
        "-d",
        "--no-recreate"
      )
    }
  }
  register("composeUp") {
    doLast {
      val projectName = project.properties["pName"] ?: throw GradleException("-PpName=name not provided")
      val versionMap = loadFile(file("docker/versions"))
      val template = "docker/template/"
      val destination = "docker/"
      generateCompose(
        versionMap,
        file("${template}compose-$projectName.yml"),
        file("${destination}compose-$projectName.yml")
      )
      composeUp(projectName)
    }
  }

  register("composeBaseUp") {
    dependsOn("generateBaseCompose")
    doLast {
      composeUp("observability")
      waitUntilRunning("observability-grafana-1")
      composeUp("data")
      waitUntilRunning("data-postgres-1")
    }
  }

  fun composeDown(projectName: Any) {
    exec {
      commandLine("docker", "compose", "-p", projectName, "down")
    }
  }

  register("composeBaseDown") {
    composeDown("data")
    composeDown("observability")
  }

  register("composeDown") {
    doLast {
      val pName = project.properties["pName"] ?: throw GradleException("-PpName=name not provided")
      composeDown(pName)
    }
  }

  register("stopContainer") {
    doLast {
      doLast {
        val container = project.properties["container"] ?: throw GradleException("-Pcontainer=name not provided")
        val pName = project.properties["pName"] ?: throw GradleException("-PpName=name not provided")
        exec {
          commandLine("docker", "stop", "$pName-$container-1")
        }
      }
    }
  }

  register("runTunnel") {
    doLast {
      exec {
        workingDir("./docker/tunnel/")
        commandLine(
          "bash", "run_tunnel.sh", "&"
        )
      }
    }
  }
}