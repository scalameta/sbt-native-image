addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.31")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value

Compile / unmanagedSourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile / "plugin" / "src" / "main" /
    "scala"
Compile / unmanagedResourceDirectories +=
  (ThisBuild / baseDirectory).value.getParentFile / "plugin" / "src" / "main" /
    "resources"
