import java.io.ByteArrayOutputStream
val versionFile: File by project.extra { file("project.version") }

tasks {
   fun push() {
      exec {
         commandLine("git", "push", "-q")
      }
   }

   fun pull() {
      exec {
         commandLine("git", "pull", "-q")
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
         val branch = project.properties["branch"] ?: throw GradleException("-Pbranch=name not provided")
         pull()
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
         bumpRelease()
         exec {
            commandLine("sh", "-c", "\"git-flow feature finish -kS $branch\"")
         }
      }
   }

   register("releaseStart") {
      doLast {
         switch("dev")
         pull()
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
}