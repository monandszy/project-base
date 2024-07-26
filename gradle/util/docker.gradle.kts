import java.nio.file.Files

tasks {

  fun generateCompose(map: Map<String, String>, templateFile: File, outputFile: File) {
    var template = templateFile.readText()
    map.forEach { (key, value) ->
      template = template.replace("\${$key}", value)
    }
    Files.write(outputFile.toPath(), template.toByteArray())
  }

  fun composeModuleUp(profile: Any, moduleName: Any) {
    val suffix = if (profile == "prod") "" else "-$profile"
    exec {
      workingDir("./docker/")
      commandLine(
        "docker", "compose",
        "-p", "$moduleName$suffix",
        "-f", "compose-$moduleName-$profile.yml",
        "up",
        "-d",
        "--force-recreate", "$moduleName$suffix"
      )
    }
  }

  register("moduleProdUp") {
    dependsOn("docker")
    doLast {
      val profile = "prod"
      val variables = HashMap<String, String>()
      variables["default-network"] = "${project.name}-$profile"
      variables["project-version"] = "$version"
      variables["project-name"] = "${project.name}"
      generateCompose(
        variables,
        file("docker/template/compose-${project.name}-$profile.yml"),
        file("docker/compose-${project.name}-$profile.yml")
      )
      composeModuleUp(profile, project.name)
    }
  }

  register("moduleDevUp") {
    dependsOn("docker")
    doLast {
      val profile = "dev"
      val variables = HashMap<String, String>()
      variables["default-network"] = "${project.name}-$profile"
      variables["project-version"] = "$version"
      variables["project-name"] = "${project.name}"
      generateCompose(
        variables,
        file("docker/template/compose-${project.name}-$profile.yml"),
        file("docker/compose-${project.name}-$profile.yml")
      )
      composeModuleUp(profile, project.name)
    }
  }

  fun composeDown(projectName: Any) {
    exec {
      commandLine("docker", "compose", "-p", projectName, "down")
    }
  }

  register("ModuleDevDown") {
    composeDown("${project.name}-dev")
  }
  register("ModuleProdDown") {
    composeDown("${project.name}-prod")
  }

  register<Exec>("extractLayers") {
    dependsOn("bootJar")
    workingDir = projectDir
    commandLine(
      "java",
      "-Djarmode=layertools",
      "-jar", "build/libs/${project.name}-${version}.jar",
      "extract",
      "--destination", "build/extracted"
    )
  }
  register("docker") {
    dependsOn("bootJar")
    dependsOn(getByName("extractLayers"))
    doLast {
      exec {
        workingDir = projectDir
        commandLine(
          "docker",
          "build",
          "--build-arg", "EXTRACTED=build/extracted",
          "-t", "${rootProject.name}/${project.name}:$version",
          "-q",
          "-f", "./docker/Dockerfile",
          "."
        )
      }
    }
  }
}