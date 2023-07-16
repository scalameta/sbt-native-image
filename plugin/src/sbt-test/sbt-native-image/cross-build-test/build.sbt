lazy val example = project
  .settings(
    crossScalaVersions := List(
      "2.11.10",
      "2.12.10",
      "2.12.18",
      "2.13.1",
      "2.13.11"
    ),
    Compile / mainClass := Some("example.Hello5"),
    nativeImageTestOptions ++= Seq(
      "--initialize-at-build-time=scala.collection.immutable.VM"
    ),
    Test / mainClass := Some("org.scalatest.tools.Runner"),
    nativeImageTestRunOptions ++= Seq("-o", "-R", (Test / classDirectory).value.absolutePath),
    nativeImageCommand := List(
      sys.env.getOrElse(
        "NATIVE_IMAGE_COMMAND",
        "missing environment variable 'NATIVE_IMAGE_COMMAND'. " +
          "To fix this problem, manually install GraalVM native-image and update the environment " +
          "variable to point to the absolute path of this binary."
      )
    ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % Test
  )
  .enablePlugins(NativeImagePlugin)
