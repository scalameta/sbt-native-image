enablePlugins(NativeImagePlugin)
nativeImageOptions += "--no-fallback"
crossScalaVersions := List(
  "2.12.21",
  "2.13.18",
)
Compile / mainClass := Some("Prog")
InputKey[Unit]("check") := {
  import sbtcompat.PluginCompat.*
  val conv0 = fileConverter.value
  implicit val conv: xsbti.FileConverter = conv0
  val binary = nativeImage.value
  val output = scala.sys.process.Process(List(toNioPath(binary).toString)).!!.trim
  assert(output == "List(1, 2, 3)", s"obtained: $output")
}
