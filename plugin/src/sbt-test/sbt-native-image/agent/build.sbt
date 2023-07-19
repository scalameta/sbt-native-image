lazy val example = project
  .settings(
    scalaVersion := "2.13.11",
    Compile / mainClass := Some("example.Hello3"),
    nativeImageOptions ++= Seq(
      "--no-fallback",
      s"-H:ReflectionConfigurationFiles=${ target.value / "native-image-configs" / "reflect-config.json" }"
    ),
    nativeImageCommand := List(
      sys.env.getOrElse(
        "NATIVE_IMAGE_COMMAND",
        "missing environment variable 'NATIVE_IMAGE_COMMAND'. " +
          "To fix this problem, manually install GraalVM native-image and update the environment " +
          "variable to point to the absolute path of this binary."
      )
    )
  )
  .enablePlugins(NativeImagePlugin)
