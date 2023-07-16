enablePlugins(NativeImagePlugin)
nativeImageOptions += "--no-fallback"
crossScalaVersions := List(
  "2.11.10",
  "2.12.10",
  "2.12.18",
  "2.13.1",
  "2.13.11"
)
Compile / mainClass := Some("Prog")
TaskKey[Unit]("check") := {
  val binary = nativeImage.value
  val output = scala.sys.process.Process(List(binary.toString)).!!.trim
  assert(output == "List(1, 2, 3)", s"obtained: $output")
}
