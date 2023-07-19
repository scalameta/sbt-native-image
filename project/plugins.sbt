addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.12")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.11.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile / "plugin" / "src" / "main" /
    "scala"
Compile / unmanagedResourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile / "plugin" / "src" / "main" /
    "resources"
