def scala212 = "2.12.20"
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
    semanticdbEnabled := true,
    semanticdbVersion := "4.13.9"
  )
)

crossScalaVersions := Nil
skip.in(publish) := true

commands +=
  Command.command("fixAll") { s =>
    "scalafixAll" :: "scalafmtAll" :: "scalafmtSbt" :: s
  }

commands +=
  Command.command("checkAll") { s =>
    "scalafmtCheckAll" :: "scalafmtSbtCheck" :: "scalafixAll --check" ::
      "publishLocal" :: s
  }

lazy val plugin = project
  .in(file("plugin"))
  .settings(
    moduleName := "sbt-native-image",
    sbtPlugin := true,
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          "1.5.8"
        case _ =>
          "2.0.0-RC4"
      }
    },
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
    skip.in(publish) := true,
    mainClass.in(Compile) := Some("example.Hello"),
    test := {
      val binary = nativeImage.value
      val output = scala.sys.process.Process(List(binary.toString)).!!.trim
      assert(output == "List(1, 2, 3)", output)
    }
  )
  .enablePlugins(NativeImagePlugin)
