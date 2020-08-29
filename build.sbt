def scala212 = "2.12.12"
inThisBuild(
  List(
    organization := "org.scalameta",
    homepage := Some(url("https://github.com/scalameta/sbt-native-image")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "olafurpg",
        "Ólafur Páll Geirsson",
        "olafurpg@gmail.com",
        url("https://geirsson.com")
      )
    ),
    scalaVersion := scala212
  )
)

crossScalaVersions := Nil
skip.in(publish) := true

lazy val plugin = project
  .in(file("plugin"))
  .settings(
    moduleName := "sbt-native-image",
    sbtPlugin := true,
    sbtVersion.in(pluginCrossBuild) := "1.0.0",
    crossScalaVersions := List(scala212)
  )

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
