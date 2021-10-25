def scala212 = "2.12.12"
inThisBuild(
  List(
    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/sbt-native-image")),
    licenses :=
      List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers :=
      List(
        Developer(
          "olafurpg",
          "Ólafur Páll Geirsson",
          "olafurpg@gmail.com",
          url("https://geirsson.com")
        )
      ),
    scalaVersion := scala212,
    scalafixDependencies +=
      "com.github.liancheng" %% "organize-imports" % "0.5.0",
    scalacOptions ++= List("-Ywarn-unused-import"),
    scalafixCaching := true,
    semanticdbEnabled := true
  )
)

crossScalaVersions := Nil
publish / skip := true

commands +=
  Command.command("fixAll") { s =>
    "scalafixAll" :: "scalafmtAll" :: "scalafmtSbt" :: s
  }

commands +=
  Command.command("checkAll") { s =>
    "scalafmtCheckAll" :: "scalafmtSbtCheck" :: "scalafixAll --check" ::
      "publishLocal" :: s
  }

Global / excludeLintKeys += crossSbtVersions
//supress [warn] * plugin / crossSbtVersions
// https://github.com/sbt/sbt/issues/6571 and will be fixed https://github.com/sbt/sbt/pull/6656
lazy val plugin = project
  .in(file("plugin"))
  .settings(
    moduleName := "sbt-native-image",
    sbtPlugin := true,
    crossSbtVersions := Vector("0.13.16", "1.0.0"),
    crossScalaVersions := List(scala212),
    buildInfoPackage := "sbtnativeimage",
    buildInfoKeys := Seq[BuildInfoKey](version),
    scriptedBufferLog := false,
    scriptedLaunchOpts ++=
      Seq("-Xmx2048M", s"-Dplugin.version=${version.value}")
  )
  .enablePlugins(ScriptedPlugin, BuildInfoPlugin)

lazy val example = project
  .in(file("example"))
  .settings(
    publish / skip := true,
    Compile / mainClass := Some("example.Hello"),
    test := {
      val binary = nativeImage.value
      val output = scala.sys.process.Process(List(binary.toString)).!!.trim
      assert(output == "List(1, 2, 3)", output)
    }
  )
  .enablePlugins(NativeImagePlugin)
