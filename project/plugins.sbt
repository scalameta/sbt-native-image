addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.4")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

unmanagedSourceDirectories.in(Compile) +=
  baseDirectory.in(ThisBuild).value.getParentFile /
    "plugin" / "src" / "main" / "scala"
unmanagedResourceDirectories.in(Compile) +=
  baseDirectory.in(ThisBuild).value.getParentFile /
    "plugin" / "src" / "main" / "resources"
