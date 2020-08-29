# sbt-native-image: generate native-image binaries with sbt

This plugin makes it easy to generate native-image binaries with sbt. Key
features:

- automatic GraalVM native-image installation via Coursier, no need to start sbt
  with GraalVM or spin up docker.
- automatic support for Scala 2.12.12+ and 2.13.3+, no need to deal with issues
  like [scala/bug#11634](https://github.com/scala/bug/issues/11634).
- get a notification when the binary is ready to use.

**Overview:**

- [Getting started](#getting-started)
- [Configuration](#configuration)
- [Generate native-image from GitHub Actions](#generate-native-image-fromgithub-actions)
- [Comparison with sbt-native-packager](#comparison-with-sbt-native-packager)

## Getting started

First, add the sbt plugin to your build in `project/plugins.sbt`.

[![](https://index.scala-lang.org/scalameta/sbt-native-image/latest.svg?color=blue)](https://github.com/scalameta/sbt-native-image/releases)

```scala
// project/plugins.sbt
addSbtPlugin("org.scalameta" % "sbt-native-image" % "0.1.1")
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

Finally, run the `nativeImage` task to generate the binary.

```sh
$ sbt
> myNativeImageProject/nativeImage
...
[info] Native image ready!
[info] /path/to/your/binary
```

Optionally, use `nativeImageRun` to execute the generated binary and manually
test that it works as expected.

```sh
> myNativeImageProject/nativeImageRun argument1 argument 2
# output from your native-image binary
```

## Configuration

sbt-native-image provides several settings, tasks and input tasks to customize
native-image generation and to automate your native-image workflows.

- [`nativeImage`](#nativeimage): generate a native image
- [`nativeImageOptions`](#nativeimageoptions): customize native image generation
- [`nativeImageRun`](#nativeimagerun): execute the generated native image
- [`nativeImageCopy`](#nativeimagecopy): generate a native image and copy the
  binary
- [`nativeImageVersion`](#nativeimageversion): the GraalVM version to use for
  native-image
- [`nativeImageCommand`](#nativeimagecommand): the command to launch
  `native-image`
- [`nativeImageReady`](#nativeimagealert): callback hook when native-image is
  ready
- [`nativeImageCoursier`](#nativeimagecoursier): the path to a `coursier` binary
- [`nativeImageOutput`](#nativeimageoutput): the path to the generated
  native-image binary

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

**Type**: `InputKey[File]`

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

### `nativeImageReady`

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

## Generate native-image from GitHub Actions

The easiest way to distribute native-image binaries for Linux and macOS is to
build the binaries in CI with GitHub Actions.

1.  Copy the `native.yml` file from this repo into the `.github/workflows/`
    directory in your project.

        mkdir -p .github/workflows && \
          curl -L https://raw.githubusercontent.com/scalameta/sbt-native-image/master/.github/workflows/ci.yml > .github/workflows/native.yml

2.  Edit the file to replace "example" with the name of your binary.
3.  Commit your changes.
4.  Push your commit to GitHub and see the binary get uploaded as an artifact to
    the CI job.
5.  Create a GitHub release and see the binary get uploaded as assets to the
    release page.

**Help wanted**: it would be lovely to add support for Windows as well. If you
know how to accomplish this, please consider contributing!.

## Comparison with sbt-native-packager

The sbt-native-packager plugin provides similar support to generate native-image
binaries. Check out their documentation at
https://sbt-native-packager.readthedocs.io/en/stable/formats/graalvm-native-image.html

The key differences between sbt-native-packager and sbt-native-image are:

- sbt-native-image automatically installs GraalVM native-image by default. You
  don't need to configure a docker image or manually install a correct GraalVM
  JDK before starting sbt.
- sbt-native-image automatically works out-of-the-box with Scala 2.12.12+ and
  2.13.3+. You don't need custom settings to work around issues like like
  [scala/bug#11634](https://github.com/scala/bug/issues/11634).
- sbt-native-image displays live progress output from the `native-image` while
  it's generating the binary. For some reason, sbt-native-packager only displays
  output from native-image after the process completes (see issue
  [#1345](https://github.com/sbt/sbt-native-packager/issues/1345)).
- sbt-native-packager has Docker support, which is helpful if you need more
  fine-grained control over the linking environment. There are no plans to add
  Docker support in sbt-native-image.
