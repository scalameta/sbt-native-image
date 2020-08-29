# sbt-native-image: generate native-image binaries with sbt

This plugin makes it easy to generate native-image binaries with sbt. Key
features:

- automatic GraalVM native-image installation via Coursier, no need to start sbt
  with GraalVM or spin up docker.
- automatic support for Scala 2.12.12+ and 2.13.3+, no need to deal with issues
  like [scala/bug#11634](https://github.com/scala/bug/issues/11634).
- get a notification when the binary is ready to use.

## Getting started

First, add the sbt plugin to your build in `project/plugins.sbt`.

```scala
// project/plugins.sbt
addSbtPlugin("org.scalameta" % "sbt-native-image" % "VERSION")
```

Next, enable the plugin to your native-image application in `build.sbt` and
configure the main class.

```diff
  // build.sbt
  lazy val myNativeImageProject = project
+   .enablePlugins(NativeImagePlugin)
    .settings(
      // ...
+     Compile / mainClass := Some("com.my.MainClass")
    )
```

Finally, run the `nativeImage` task to generate the binary and run the
`nativeImageRun` input task to execute the binary.

```sh
$ sbt
> myNativeImageProject/nativeImage
...
[info] Native image ready!
[info] /path/to/your/binary
> myNativeImageProject/nativeImageRun argument1 argument 2
# output from your native-image binary
```

## Documentation

sbt-native-image provides several settings, tasks and input tasks to customize
native-image generation and to automate your native-image workflows.

### `nativeImage`

**Type**: `TaskKey[File]`

**Description**: runs native-image and returns the resulting binary file.

**Example usage**: `sbt myProject/nativeImage`

### `nativeImageOptions`

**Type**: `TaskKey[Seq[String]]`

**Description**: custom native-image linking options. See `native-image --help`
for available options. Empty by default.

**Example usage**: `nativeImageOptions ++= List("--initialize-at-build-time")`

### `nativeImageRun`

**Type**: `TaskKey[File]`

**Description**: executes the native-image binary with given arguments. This
task can only be used after `nativeImage` has completed.

**Example usage**:

- `sbt myProject/nativeImage 'myProject/nativeImageRun hello'`
- Error: `sbt clean myProject/nativeImageRun`. Crashes because native-image does
  not exist.

### `nativeImageCopy`

**Type**: `InputKey[File]`

**Description**: identical to `nativeImage` except the resulting binary is
additionally copied to the provided file. This task is helpful when configuring
CI to generate the binary in a specific place.

**Example usage**:

- `sbt 'myProject/nativeImageCopy mybinary-x86_64-apple-darwin'`.
- `sbt 'myProject/nativeImageCopy mybinary-x86_64-pc-linux'`.

### `nativeImageVersion`

**Type**: `SettingKey[String]`

**Description**: the GraalVM version to use.

**Default**: 20.1.0

**Example usage**: `nativeImageVersion := "19.3.3"`

### `nativeImageCommand`

**Type**: `TaskKey[Seq[String]]`

**Description**: the base command that is used to launch native-image.

**Default**: launches GraalVM via Coursier. Can be customized to execute custom
`native-image` binary directly.

**Example usage**: `nativeImageCommand := List("/path/to/native-image")`

### `nativeImageAlert`

**Type**: `SettingKey[() => Unit]`

**Description**: a side-effecting callback that is called the native image is
ready.

**Default**: alerts the message "Native image ready!" via the `say` command-line
tool on macOS. Does nothing by default on Linux and Windows.

### `nativeImageCoursier`

**Type**: `TaskKey[File]`

**Description**: the path to a `coursier` binary.

**Default**: copies a slim bootstrap binary from sbt-native-image resources.
This setting is ignored if you customize `nativeImageCommand` to use something
else than Coursier.

### `nativeImageOutput`

**Type**: `SettingKey[File]`

**Description**: the path to the native-image binary that is generated.

**Default**: `target/native-image/NAME` where `NAME` is the name of the sbt
project. for available options.

**Example usage**: `nativeImageOutput := file("target") / "my-binary"`
