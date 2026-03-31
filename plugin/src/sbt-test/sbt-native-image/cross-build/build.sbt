enablePlugins(NativeImagePlugin)
nativeImageOptions += "--no-fallback"
crossScalaVersions := List(
  "2.12.21",
  "2.13.18",
)
Compile / mainClass := Some("Prog")
TaskKey[Unit]("check") := {
  val binary = nativeImage.value
  val output = scala.sys.process.Process(List(binary.toString)).!!.trim
  assert(output == "List(1, 2, 3)", s"obtained: $output")
}
