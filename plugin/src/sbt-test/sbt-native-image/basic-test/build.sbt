lazy val example = project
  .settings(
    scalaVersion := "2.12.12",
    mainClass.in(Compile) := Some("example.Hello4"),
    nativeImageCommand := List(
      sys.env.getOrElse(
        "NATIVE_IMAGE_COMMAND",
        "missing environment variable 'NATIVE_IMAGE_COMMAND'. " +
          "To fix this problem, manually install GraalVM native-image and update the environment " +
          "variable to point to the absolute path of this binary."
      )
    ),
    nativeImageTestOptions ++= Seq(
      "--initialize-at-build-time=scala.collection.immutable.VM"
    ),
    mainClass.in(Test) := Some("org.scalatest.tools.Runner"),
    nativeImageTestRunOptions ++= Seq("-o", "-R", classDirectory.in(Test).value.absolutePath),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
  )
  .enablePlugins(NativeImagePlugin)
