enablePlugins(NativeImagePlugin)
nativeImageOptions += "--no-fallback"
crossScalaVersions := List(
  "2.11.10",
  "2.12.10",
  "2.12.12",
  "2.13.1",
  "2.13.3"
)
mainClass.in(Compile) := Some("Prog")
TaskKey[Unit]("check") := {
  val binary = nativeImage.value
  val output = scala.sys.process.Process(List(binary.toString)).!!.trim
  assert(output == "List(1, 2, 3)", s"obtained: $output")
}
