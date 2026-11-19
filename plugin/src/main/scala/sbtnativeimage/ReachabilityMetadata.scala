package sbtnativeimage

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import sbt.IO
import sbt.MessageOnlyException
import sbt.ModuleID
import sjsonnew.shaded.scalajson.ast.unsafe._
import sjsonnew.support.scalajson.unsafe.Parser

private object ReachabilityMetadata {
  private case class Module(directory: Option[String], requires: Seq[String])

  def withOptions[A](inputs: Option[(File, Seq[ModuleID])])(
      run: Seq[String] => A
  ): A = inputs match {
    case None => run(Nil)
    case Some((archive, dependencies)) =>
      IO.withTemporaryDirectory { directory =>
        val options =
          try selectAndExtract(archive, dependencies, directory.toPath)
          catch {
            case NonFatal(error) =>
              throw new MessageOnlyException(
                s"Could not read GraalVM reachability metadata from $archive: ${error.getMessage}"
              )
          }
        run(options)
      }
  }

  private def selectAndExtract(
      archive: File,
      dependencies: Seq[ModuleID],
      output: Path
  ): Seq[String] = {
    val zip = new ZipFile(archive)
    try {
      val modules = readIndex(zip, "index.json").map { entry =>
        val name = string(entry("module"))
        val directory = entry.get("directory").map(string)
        val requires = strings(entry, "requires")
        name -> Module(directory, requires)
      }.toMap

      val resolved = dependencies.map { dependency =>
        s"${dependency.organization}:${dependency.name}" -> dependency.revision
      }
      val resolvedNames = resolved.map(_._1).toSet
      val queries = resolved.flatMap { case (name, version) =>
        val required = modules.get(name).toSeq.flatMap(_.requires)
        val additional = required.filterNot(resolvedNames).map(_ -> version)
        (name -> version) +: additional
      }
      val indexes = scala.collection.mutable.Map.empty[String, Seq[Map[String, JValue]]]
      val prefixes = queries.distinct.flatMap { case (name, version) =>
        modules.get(name).flatMap(_.directory).flatMap { directory =>
          val entries = indexes.getOrElseUpdate(directory, readIndex(zip, s"$directory/index.json"))
          val exact = entries.find(entry => strings(entry, "tested-versions").contains(version))
          val fallback = exact.orElse {
            entries.find(_.get("default-for").exists(pattern => version.matches(string(pattern))))
          }
          val selected = fallback.orElse(entries.find(_.get("latest").contains(JTrue)))
          selected.flatMap(_.get("metadata-version")).map(v => s"$directory/${string(v)}")
        }
      }.distinct.sorted

      val entries = zip.entries().asScala.filterNot(_.isDirectory).toVector
      prefixes.map { prefix =>
        val directory = inside(output, prefix)
        val files = entries.filter(_.getName.startsWith(prefix + "/"))
        require(files.nonEmpty, s"No files in metadata directory '$prefix'")
        files.foreach { entry =>
          val path = inside(directory, entry.getName.substring(prefix.length + 1))
          Files.createDirectories(path.getParent)
          val stream = zip.getInputStream(entry)
          try Files.copy(stream, path)
          finally stream.close()
        }
        s"-H:ConfigurationFileDirectories=$directory"
      }
    } finally zip.close()
  }

  private def inside(root: Path, child: String): Path = {
    val path = root.resolve(child).normalize()
    require(path != root && path.startsWith(root), s"Invalid metadata path '$child'")
    path
  }

  private def string(value: JValue): String = value match {
    case JString(text) => text
    case _ => throw new IllegalArgumentException("Expected a metadata string")
  }

  private def strings(entry: Map[String, JValue], key: String): Seq[String] =
    entry.get(key) match {
      case None => Nil
      case Some(JArray(values)) => values.iterator.map(string).toVector
      case _ => throw new IllegalArgumentException(s"Expected an array for '$key'")
    }

  private def readIndex(zip: ZipFile, name: String): Seq[Map[String, JValue]] = {
    val entry = Option(zip.getEntry(name)).getOrElse {
      throw new IllegalArgumentException(s"Missing '$name'")
    }
    val stream = zip.getInputStream(entry)
    val contents = try IO.readStream(stream, StandardCharsets.UTF_8)
    finally stream.close()
    Parser.parseFromString(contents).get match {
      case JArray(values) =>
        values.iterator.map {
          case JObject(fields) => fields.iterator.map { case JField(key, value) => key -> value }.toMap
          case _ => throw new IllegalArgumentException(s"Expected an object in '$name'")
        }.toVector
      case _ => throw new IllegalArgumentException(s"Expected an array in '$name'")
    }
  }
}
