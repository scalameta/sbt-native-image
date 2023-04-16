lazy val example = project
  .settings(
    crossScalaVersions := List(
      "2.11.10",
      "2.12.10",
      "2.12.12",
      "2.13.1",
      "2.13.3"
    ),
    mainClass.in(Compile) := Some("example.Hello5"),
    nativeImageTestOptions ++= Seq(
      "--initialize-at-build-time=scala.collection.immutable.VM"
    ),
    mainClass.in(Test) := Some("org.scalatest.tools.Runner"),
    nativeImageTestRunOptions ++= Seq("-o", "-R", classDirectory.in(Test).value.absolutePath),
    nativeImageCommand := List(
      sys.env.getOrElse(
        "NATIVE_IMAGE_COMMAND",
        "missing environment variable 'NATIVE_IMAGE_COMMAND'. " +
          "To fix this problem, manually install GraalVM native-image and update the environment " +
          "variable to point to the absolute path of this binary."
      )
    ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.14" % "test"
  )
  .enablePlugins(NativeImagePlugin)
