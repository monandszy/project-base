import nebula.plugin.release.git.opinion.Strategies

plugins {
   id("com.netflix.nebula.release") version "19.0.9"
   /*
   gradlew final -Prelease.version=0.1.0
   */
}



tasks {
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
//   addReleaseBranchPattern("")
//   addExcludeBranchPattern("")

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