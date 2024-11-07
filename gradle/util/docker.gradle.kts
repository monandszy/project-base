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
        "-f", "compose-$profile.yml",
        "up",
        "-d",
        "--force-recreate", "$moduleName$suffix"
      )
    }
  }

  register("moduleProdUp") {
    dependsOn("moduleProdDown")
    dependsOn("dockerBuild")
    doLast {
      val profile = "prod"
      val variables = HashMap<String, String>()
      variables["default-network"] = "${project.name}-$profile"
      variables["project-version"] = "$version"
      variables["project-name"] = "${project.name}"
      generateCompose(
        variables,
        file("docker/template/compose-$profile.yml"),
        file("docker/compose-$profile.yml")
      )
      composeModuleUp(profile, project.name)
    }
  }

  register("moduleDevUp") {
    dependsOn("moduleDevDown")
    dependsOn("dockerBuild")
    doLast {
      val profile = "dev"
      val variables = HashMap<String, String>()
      variables["default-network"] = "${project.name}-$profile"
      variables["project-version"] = "$version"
      variables["project-name"] = "${project.name}"
      generateCompose(
        variables,
        file("docker/template/compose-$profile.yml"),
        file("docker/compose-$profile.yml")
      )
      composeModuleUp(profile, project.name)
    }
  }

  fun composeDown(projectName: Any) {
    exec {
      commandLine("docker", "compose", "-p", projectName, "down")
    }
  }

  register("moduleDevDown") {
    doLast {
      composeDown("${project.name}-dev")
    }
  }
  register("moduleProdDown") {
    doLast {
      composeDown("${project.name}")
    }
  }

  register("dockerBuild") {
    dependsOn("bootJar")
    doLast {
      exec {
        workingDir = projectDir
        commandLine(
          "docker",
          "build",
          "--build-arg", "JAR_PATH=build/libs/${project.name}-${version}.jar",
          "-t", "${rootProject.name}/${project.name}:$version",
          "-q",
          "-f", "./docker/Dockerfile",
          "."
        )
      }
    }
  }
}