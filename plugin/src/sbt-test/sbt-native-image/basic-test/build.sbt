lazy val example = project
  .settings(
    scalaVersion := "2.12.18",
    Compile / mainClass := Some("example.Hello4"),
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
    Test / mainClass := Some("org.scalatest.tools.Runner"),
    nativeImageTestRunOptions ++= Seq("-o", "-R", (Test / classDirectory).value.absolutePath),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % Test
  )
  .enablePlugins(NativeImagePlugin)
