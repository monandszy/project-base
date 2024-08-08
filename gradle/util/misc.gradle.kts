tasks {
  register("printVersion") {
    doLast {
      println("Project version is ${project.version}")
    }
  }
}