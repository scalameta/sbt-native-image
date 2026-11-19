enablePlugins(NativeImagePlugin)

scalaVersion := "2.12.21"
Compile / javacOptions ++= Seq("--release", "17")
Compile / mainClass := Some("MetadataSmoke")
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.12"

nativeImageReachabilityMetadata := true
nativeImageOptions += "-Ob"
nativeImageCommand := Seq(sys.env("NATIVE_IMAGE_COMMAND"))
