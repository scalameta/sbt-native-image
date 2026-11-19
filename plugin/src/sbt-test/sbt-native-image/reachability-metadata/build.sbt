enablePlugins(NativeImagePlugin)

scalaVersion := "2.12.21"
Compile / mainClass := Some("Main")
Test / mainClass := Some("Main")

libraryDependencies ++= Seq(
  "junit" % "junit" % "4.13.2",
  "org.apache.commons" % "commons-lang3" % "3.17.0" % Test
)

val writeMetadataRepository = taskKey[Unit]("Create the fixture metadata repository")
val repository = settingKey[File]("Fixture Maven repository")
repository := baseDirectory.value / "repository"
resolvers += "metadata fixture" at repository.value.toURI.toString
nativeImageReachabilityMetadataVersion := "fixture"

writeMetadataRepository := {
  val name = "graalvm-reachability-metadata"
  val destination = repository.value / "org" / "graalvm" / "buildtools" / name / "fixture"
  IO.write(
    destination / s"$name-fixture.pom",
    s"""<project>
       |<modelVersion>4.0.0</modelVersion>
       |<groupId>org.graalvm.buildtools</groupId>
       |<artifactId>$name</artifactId>
       |<version>fixture</version>
       |</project>""".stripMargin
  )
  val entries = List(
    "index.json" -> """[
      {"module":"org.hamcrest:hamcrest-core","directory":"hamcrest","requires":["fixture:required"]},
      {"module":"fixture:required","directory":"required"},
      {"module":"org.apache.commons:commons-lang3","directory":"test-only"}
    ]""",
    "hamcrest/index.json" -> """[
      {"metadata-version":"exact","tested-versions":["1.3"]},
      {"metadata-version":"fallback","latest":true}
    ]""",
    "required/index.json" -> """[{"metadata-version":"required","latest":true}]""",
    "test-only/index.json" -> """[{"metadata-version":"regex","default-for":"3[.]17[.].*"}]""",
    "hamcrest/exact/reflect-config.json" -> "[]",
    "hamcrest/fallback/reflect-config.json" -> "[]",
    "required/required/reflect-config.json" -> "[]",
    "test-only/regex/reflect-config.json" -> "[]"
  )
  val zip = new java.util.zip.ZipOutputStream(
    new java.io.FileOutputStream(destination / s"$name-fixture-repository.zip")
  )
  try entries.foreach { case (path, contents) =>
    zip.putNextEntry(new java.util.zip.ZipEntry(path))
    zip.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8))
    zip.closeEntry()
  } finally zip.close()
}

val expectedMetadata = settingKey[String]("Expected metadata directories in linker arguments")
expectedMetadata := ""
Test / expectedMetadata := "hamcrest/exact,required/required,test-only/regex"

nativeImageCommand := Seq(
  file(sys.props("java.home")).toPath.resolve("bin").resolve("java").toString,
  (baseDirectory.value / "CheckMetadata.java").absolutePath,
  expectedMetadata.value
)
nativeImageTestCommand := Seq(
  file(sys.props("java.home")).toPath.resolve("bin").resolve("java").toString,
  (baseDirectory.value / "CheckMetadata.java").absolutePath,
  (Test / expectedMetadata).value
)
