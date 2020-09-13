enablePlugins(NativeImagePlugin)
commands += Command.command("updateScalaVersion") { s =>
  "set every scalaVersion := \"2.12.12\"" :: s
}
mainClass.in(Compile) := Some("Prog")
