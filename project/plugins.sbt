addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.6")

addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0")

Compile / scalacOptions += "-Xsource:3"
Compile / unmanagedSourceDirectories ++=
  List(
    baseDirectory.in(ThisBuild).value.getParentFile / "plugin" / "src" /
      "main" / "scala",
    baseDirectory.in(ThisBuild).value.getParentFile / "plugin" / "src" /
      "main" / "scala-2.12"
  )

Compile / unmanagedResourceDirectories +=
  baseDirectory.in(ThisBuild).value.getParentFile / "plugin" / "src" / "main" /
    "resources"
