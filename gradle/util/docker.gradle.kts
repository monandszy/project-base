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
            "-p", moduleName,
            "-f", "$moduleName/docker/compose-$profile.yml",
            "up",
            "-d",
            "--force-recreate", "$moduleName"
         )
      }
   }

   register("moduleDevUp") {
      dependsOn("docker")
      doLast {
         val variables = HashMap<String, String>()
         variables["app-version"] = "$version"
         generateCompose(variables,
            file("docker/template/compose-dev"),
            file("docker/compose-dev")
         )
         val pName = project.properties["pName"] ?: throw GradleException("-PpName=name not provided")
         composeModuleUp("dev", pName)
      }
   }

   register("moduleProdUp") {
      dependsOn("docker")
      doLast {
         val variables = HashMap<String, String>()
         variables["app-version"] = "$version"
         generateCompose(variables,
            file("docker/template/compose-prod"),
            file("docker/compose-prod")
         )
         val pName = project.properties["pName"] ?: throw GradleException("-PpName=name not provided")
         composeModuleUp("prod", pName)
      }
   }
}