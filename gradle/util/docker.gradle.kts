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
      exec {
         workingDir("./docker/")
         commandLine(
            "docker", "compose",
            "-p", profile,
            "-f", "$moduleName/docker/compose-$profile.yml",
            "up",
            "-d",
            "--force-recreate", "$moduleName"
         )
      }
   }

   register("moduleUp") {
      dependsOn("docker")
      doLast {
         val profile = project.properties["profile"] ?: throw GradleException("-Pprofile=name not provided")
         val variables = HashMap<String, String>()
         variables["app-version"] = "$version"
         variables["profile"] = "$profile"
         generateCompose(variables,
            file("docker/compose-app-template.yml"),
            file("docker/compose-app.yml")
         )
         composeModuleUp(profile, project.name)
      }
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
}