package sbtnativeimage

import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers._
import sbt.plugins.JvmPlugin

import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.jar.{Attributes, JarOutputStream, Manifest}
import scala.collection.mutable
import scala.sys.process.Process
import scala.util.Properties
import scala.util.control.NonFatal

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
    lazy val nativeImageJvm: SettingKey[String] =
      settingKey[String](
        "The GraalVM JVM version, one of: graalvm-java11 (default) | graalvm (Java 8)"
      )
    lazy val nativeImageJvmIndex: SettingKey[String] =
      settingKey[String](
        "The JVM version index to use, one of: cs (default) | jabba"
      )
    lazy val nativeImageCoursier: TaskKey[File] =
      taskKey[File](
        "Path to a coursier binary that is used to launch GraalVM native-image."
      )
    lazy val nativeImageInstalled: SettingKey[Boolean] =
      settingKey[Boolean]("Whether GraalVM is manually installed or should be downloaded with coursier.")
    lazy val nativeImageGraalHome: TaskKey[Path] =
      taskKey[Path]("Path to GraalVM home directory.")
    lazy val nativeImageCommand: TaskKey[Seq[String]] =
      taskKey[Seq[String]](
        "The command arguments to launch the GraalVM native-image binary."
      )
    lazy val nativeImageRunAgent: InputKey[Unit] =
      inputKey[Unit](
        "Run application, tracking all usages of dynamic features of an execution with `native-image-agent`."
      )
    lazy val nativeImageAgentOutputDir: SettingKey[File] =
      settingKey[File](
        "Directory where `native-image-agent` should put generated configurations."
      )
    lazy val nativeImageAgentMerge: SettingKey[Boolean] =
      settingKey[Boolean](
        "Whether `native-image-agent` should merge generated configurations." +
          s" (See $assistedConfigurationOfNativeImageBuildsLink for details)"
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

    private lazy val assistedConfigurationOfNativeImageBuildsLink =
      "https://www.graalvm.org/reference-manual/native-image/BuildConfiguration/#assisted-configuration-of-native-image-builds"
  }

  import autoImport._

  private def copyResource(
                            filename: String,
                            outDir: File
                          ): File = {
    Files.createDirectories(outDir.toPath)
    val in =
      this.getClass().getResourceAsStream(s"/sbt-native-image/${filename}")
    if (in == null) {
      throw new MessageOnlyException(
        "unable to find coursier binary via resources. " +
          "To fix this problem, define the `nativeImageCoursier` task to return the path to a Coursier binary."
      )
    }
    val out = outDir.toPath.resolve(filename)
    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING)
    out.toFile.setExecutable(true)
    out.toFile
  }

  override lazy val projectSettings: Seq[Def.Setting[_]] = List(
    libraryDependencies += "org.scalameta" % "svm-subs" % "101.0.0",
    target.in(NativeImage) :=
      target.in(Compile).value / "native-image",
    target.in(NativeImageInternal) :=
      target.in(Compile).value / "native-image-internal",
    nativeImageReady := {
      val s = streams.value

      { () => this.alertUser(s, "Native image ready!") }
    },
    mainClass.in(NativeImage) := mainClass.in(Compile).value,
    nativeImageJvm := "graalvm-java11",
    nativeImageJvmIndex := "cs",
    nativeImageVersion := "20.2.0",
    name.in(NativeImage) := name.value,
    mainClass.in(NativeImage) := mainClass.in(Compile).value,
    nativeImageOptions := List(),
    nativeImageCoursier := {
      val dir = target.in(NativeImageInternal).value
      val out = copyResource("coursier", dir)
      if (Properties.isWin) {
        copyResource("coursier.bat", dir)
      } else {
        out
      }
    },
    nativeImageInstalled := Def.settingDyn {
      val installed =
        "true".equalsIgnoreCase(System.getProperty("native-image-installed")) ||
          "true".equalsIgnoreCase(System.getenv("NATIVE_IMAGE_INSTALLED")) ||
          "true".equalsIgnoreCase(System.getProperty("graalvm-installed")) ||
          "true".equalsIgnoreCase(System.getenv("GRAALVM_INSTALLED"))
      Def.setting(installed)
    }.value,
    nativeImageGraalHome := Def.taskDyn {
      if (nativeImageInstalled.value) {
        val path = Paths.get {
          List("GRAAL_HOME", "GRAALVM_HOME", "JAVA_HOME").iterator
            .map(key => Option(System.getenv(key)))
            .collectFirst { case Some(value) => value }
            .getOrElse("")
        }
        Def.task(path)
      } else {
        Def.task {
          val coursier = nativeImageCoursier.value.absolutePath
          val svm = nativeImageVersion.value
          val jvm = nativeImageJvm.value
          val index = nativeImageJvmIndex.value
          Paths.get(
            Process(
              List(
                coursier,
                "java-home",
                "--jvm-index",
                index,
                "--jvm",
                s"$jvm:$svm"
              )
            ).!!.trim
          )
        }
      }
    }.value,
    nativeImageCommand := Def.taskDyn {
      val graalHome = nativeImageGraalHome.value
      if (nativeImageInstalled.value) {
        val binary =
          if (Properties.isWin) "native-image.cmd" else "native-image"
        val path =
          graalHome.resolve("bin").resolve(binary)
        Def.task(List[String](path.toString()))
      } else {
        Def.task {
          val cmd = if (Properties.isWin) ".cmd" else ""
          val ni = graalHome.resolve("bin").resolve(s"native-image$cmd")
          if (!Files.isExecutable(ni)) {
            val gu = ni.resolveSibling(s"gu$cmd")
            Process(List(gu.toString, "install", "native-image")).!
          }
          if (!Files.isExecutable(ni)) {
            throw new MessageOnlyException(
              "Failed to automatically install native-image. " +
                "To fix this problem, install native-image manually and start sbt with " +
                "the environment variable 'NATIVE_IMAGE_INSTALLED=true'"
            )
          }
          List(ni.toString())
        }
      }
    }.value,
    nativeImageAgentOutputDir := target.value / "native-image-configs",
    nativeImageAgentMerge := false,
    nativeImageRunAgent := {
      val graalHome = nativeImageGraalHome.value.toFile
      val agentConfig = if (nativeImageAgentMerge.value) "config-merge-dir" else "config-output-dir"
      val agentOption = s"-agentlib:native-image-agent=$agentConfig=${nativeImageAgentOutputDir.value}"
      val tpr = thisProjectRef.value
      val settings = Seq(
        fork in(tpr, Compile, run) := true,
        javaHome in(tpr, Compile, run) := Some(graalHome),
        javaOptions in(tpr, Compile, run) += agentOption
      )
      val state0 = state.value
      val extracted = Project.extract(state0)
      val newState = extracted.append(settings, state0)
      val arguments = spaceDelimited("<arg>").parsed
      val input = if (arguments.isEmpty) "" else arguments.mkString(" ")
      Project.extract(newState).runInputTask(run in(tpr, Compile), input, newState)
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
      manifest.getParentFile().mkdirs()
      createManifestJar(manifest, cp)
      val nativeClasspath = manifest.absolutePath

      // Assemble native-image argument list.
      val command = mutable.ListBuffer.empty[String]
      command ++= nativeImageCommand.value
      command += "-cp"
      command += nativeClasspath
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
      cp.map(addTrailingSlashToDirectories(manifestJar)).mkString(" ")
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

  private def addTrailingSlashToDirectories(manifestJar: File)(path: File): String = {
    val syntax = if (Properties.isWin) {
      // NOTE(danirey): absolute paths are not supported by all JDKs on Windows, therefore using relative paths
      // relative paths may not be URL-encoded, otherwise an absolute path is constructed
      val manifestPath = Paths.get(manifestJar.getParent)
      val dependencyPath = Paths.get(path.getPath)
      try {
        manifestPath.relativize(dependencyPath).toString
      } catch {
        //java.lang.IllegalArgumentException: 'other' has different root
        //this happens if the dependency jar resides on a different drive then the manifest, i.e. C:\Coursier\Cache and D:\myapp\target
        //copy dependency next to manifest as fallback
        case _: IllegalArgumentException =>
          import java.nio.file.Files
          import java.nio.file.StandardCopyOption
          Files.copy(dependencyPath, manifestPath.resolve(path.getName), StandardCopyOption.REPLACE_EXISTING)
          path.getName
      }
    } else {
      // NOTE(olafur): manifest jars must use URL-encoded paths.
      // https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html
      path.toURI.toURL.getPath
    }

    val separatorAdded = {
      if (syntax.endsWith(".jar") || syntax.endsWith(File.separator)) {
        syntax
      } else {
        syntax + File.separator
      }
    }
    separatorAdded
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
