package sbtnativeImage

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.Attributes
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import scala.collection.mutable
import scala.sys.process.Process
import scala.util.Properties
import scala.util.control.NonFatal

import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin
import sbt.complete.DefaultParsers._

object NativeImagePlugin extends AutoPlugin {
  override def requires = JvmPlugin
  object autoImport {
    val NativeImage: Configuration = config("native-image")
    val NativeImageInternal: Configuration =
      config("native-image-internal").hide

    lazy val nativeImageReady: TaskKey[() => Unit] =
      taskKey[() => Unit](
        "This function is called when the native image is ready."
      )
    lazy val nativeImageVersion: SettingKey[String] =
      settingKey[String]("The version of GraalVM to use by default.")
    lazy val nativeImageCoursier: TaskKey[File] =
      taskKey[File](
        "Path to a coursier binary that is used to launch GraalVM native-image."
      )
    lazy val nativeImageCommand: TaskKey[Seq[String]] =
      taskKey[Seq[String]](
        "The command arguments to launch the GraalVM native-image binary."
      )
    lazy val nativeImage: TaskKey[File] =
      taskKey[File]("Generate a native image for this project.")
    lazy val nativeImageRun: InputKey[Unit] =
      inputKey[Unit]("Run the generated native-image binary without linking.")
    lazy val nativeImageCopy: InputKey[Unit] =
      inputKey[Unit](
        "Link the native image and copy the resulting binary to the provided file argument."
      )
    lazy val nativeImageOutput: SettingKey[File] =
      settingKey[File]("The binary that is produced by native-image")
    lazy val nativeImageOptions: TaskKey[Seq[String]] =
      taskKey[Seq[String]](
        "Extra command-line arguments that should be forwarded to the native-image optimizer."
      )
  }
  import autoImport._
  override lazy val projectSettings: Seq[Def.Setting[_]] = List(
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.11")) Nil
      else List("org.scalameta" %% "svm-subs" % nativeImageVersion.value)
    },
    target.in(NativeImage) :=
      target.in(Compile).value / "native-image",
    target.in(NativeImageInternal) :=
      target.in(Compile).value / "native-image-internal",
    nativeImageReady := {
      val s = streams.value

      { () => this.alertUser(s, "Native image ready!") }
    },
    mainClass.in(NativeImage) := mainClass.in(Compile).value,
    nativeImageVersion := "20.1.0",
    name.in(NativeImage) := name.value,
    mainClass.in(NativeImage) := mainClass.in(Compile).value,
    nativeImageOptions := List(),
    nativeImageCoursier := {
      val out = target.in(NativeImageInternal).value / "coursier"
      Files.createDirectories(out.toPath.getParent)
      val in =
        this.getClass().getResourceAsStream("/sbt-native-image/coursier")
      if (in == null) {
        throw new MessageOnlyException(
          "unable to find coursier binary via resources. " +
            "To fix this problem, define the `nativeImageCoursier` task to return the path to a Coursier binary."
        )
      }
      Files.copy(
        in,
        out.toPath,
        StandardCopyOption.REPLACE_EXISTING
      )
      out.setExecutable(true)
      out
    },
    nativeImageCommand := {
      val svmVersion = nativeImageVersion.value
      List(
        nativeImageCoursier.value.absolutePath,
        "launch",
        "--jvm",
        s"graalvm:$svmVersion",
        s"org.graalvm.nativeimage:svm-driver:$svmVersion",
        "--"
      )
    },
    nativeImageOutput :=
      target.in(NativeImage).value / name.in(NativeImage).value,
    nativeImageCopy := {
      val binary = nativeImage.value
      val out = fileParser(baseDirectory.in(ThisBuild).value).parsed
      Files.copy(
        binary.toPath(),
        out.toPath(),
        StandardCopyOption.REPLACE_EXISTING
      )
      println(out.absolutePath)
    },
    nativeImageRun := {
      val binary = nativeImageOutput.value
      if (!binary.isFile()) {
        throw new MessageOnlyException(
          s"no such file: $binary.\nTo fix this problem, run 'nativeImage' first."
        )
      }
      val arguments = spaceDelimited("<arg>").parsed.toList
      val exit = Process(binary.absolutePath :: arguments).!
      if (exit != 0) {
        throw new MessageOnlyException(s"non-zero exit: $exit")
      }
    },
    nativeImage := {
      val _ = compile.in(Compile).value
      val main = mainClass.in(NativeImage).value
      val binaryName = nativeImageOutput.value
      val cp = fullClasspath.in(Compile).value.map(_.data)
      // NOTE(olafur): we pass in a manifest jar instead of the full classpath
      // for two reasons:
      // * large classpaths quickly hit on the "argument list too large"
      //   error, especially on Windows.
      // * we print the full command to the console and the manifest jar makes
      //   it more readable and easier to copy-paste.
      val manifest = target.in(NativeImageInternal).value / "manifest.jar"
      createManifestJar(manifest, cp)

      // Assemble native-image argument list.
      val command = mutable.ListBuffer.empty[String]
      command ++= nativeImageCommand.value
      command += "-cp"
      command += manifest.absolutePath
      command ++= nativeImageOptions.value
      command += main.getOrElse(
        throw new MessageOnlyException(
          "no mainClass is specified. " +
            "To fix this problem, update build.sbt to include the settings " +
            "`mainClass.in(Compile) := Some(\"com.MainClass\")`"
        )
      )
      command += binaryName.absolutePath

      // Start native-image linker.
      streams.value.log.info(command.mkString(" "))
      val cwd = target.in(NativeImage).value
      cwd.mkdirs()
      val exit = Process(command, cwd = Some(cwd)).!
      if (exit != 0) {
        throw new MessageOnlyException(
          s"native-image command failed with exit code '$exit'"
        )
      }

      nativeImageReady.value.apply()
      streams.value.log.info(binaryName.absolutePath)
      binaryName
    }
  )

  private def isCI = "true".equalsIgnoreCase(System.getenv("CI"))

  private def createManifestJar(manifestJar: File, cp: Seq[File]): Unit = {
    // Add trailing slash to directories so that manifest dir entries work
    val classpathStr =
      cp.map(addTrailingSlashToDirectories).mkString(" ")
    val manifest = new Manifest()
    manifest.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    manifest.getMainAttributes.put(Attributes.Name.CLASS_PATH, classpathStr)
    val out = Files.newOutputStream(manifestJar.toPath)
    // This needs to be declared since jos itself should be set to close as well.
    var jos: JarOutputStream = null
    try {
      jos = new JarOutputStream(out, manifest)
    } finally {
      if (jos == null) {
        out.close()
      } else {
        jos.close()
      }
    }
  }

  private def addTrailingSlashToDirectories(path: File): String = {
    // NOTE(olafur): manifest jars must use URL-encoded paths.
    // https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html
    val syntax = path.toURI.toURL.getPath
    val separatorAdded = {
      if (syntax.endsWith(".jar")) {
        syntax
      } else {
        syntax + File.separator
      }
    }
    if (Properties.isWin) {
      // Prepend drive letters in windows with slash
      if (separatorAdded.indexOf(":") != 1) separatorAdded
      else File.separator + separatorAdded
    } else {
      separatorAdded
    }
  }

  private def alertUser(streams: std.TaskStreams[_], message: String): Unit = {
    streams.log.info(message)
    if (isCI) return
    try {
      if (Properties.isMac) {
        Process(List("say", message)).!
      }
      // NOTE(olafur): feel free to add support for Linux/Windows.
    } catch {
      case NonFatal(_) =>
    }
  }
}
