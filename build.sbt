def scala212 = "2.12.20"
def scala3 = "3.8.2"

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
    scalafixCaching := true,
    semanticdbEnabled := true,
    semanticdbVersion := "4.13.9"
  )
)

crossScalaVersions := Nil
publish / skip := true

commands +=
  Command.command("fixAll") { s =>
    "scalafixAll" :: "+ scalafmtAll" :: "scalafmtSbt" :: s
  }

commands +=
  Command.command("checkAll") { s =>
    "+ scalafmtCheckAll" :: "scalafmtSbtCheck" :: "scalafixAll --check" ::
      "+ publishLocal" :: s
  }

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin, BuildInfoPlugin)
  .settings(
    moduleName := "sbt-native-image",
    addSbtPlugin("com.github.sbt" % "sbt2-compat" % "0.1.0"),
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" =>
          Seq(
            "-release:8",
            "-Xlint",
            "-Ywarn-unused-import",
            "-Werror",
            "-Xsource:3",
            "-feature"
          )
        case "3" =>
          Nil
      }
    },
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          "1.5.8"
        case _ =>
          "2.0.0-RC10"
      }
    },
    scriptedSbt := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          "1.10.6"
        case _ =>
          (pluginCrossBuild / sbtVersion).value
      }
    },
    crossScalaVersions := List(scala212, scala3),
    buildInfoPackage := "sbtnativeimage",
    buildInfoKeys := Seq[BuildInfoKey](version),
    scriptedBufferLog := false,
    scriptedLaunchOpts ++=
      Seq("-Xmx2048M", s"-Dplugin.version=${version.value}")
  )

lazy val example = project
  .in(file("example"))
  .settings(
    publish / skip := true,
    Compile / mainClass := Some("example.Hello"),
    test := {
      val binary = nativeImage.value
      val output = scala.sys.process.Process(List(binary.toString)).!!.trim
      assert(output == "List(1, 2, 3)", output)
    },
    scalacOptions ++= {
      scalaBinaryVersion.value match {
        case "2.12" =>
          Seq("-release:8", "-Xlint", "-Ywarn-unused-import", "-Werror")
        case "3" =>
          Nil
      }
    }
  )
  .enablePlugins(NativeImagePlugin)
