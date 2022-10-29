package example

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

class Hello5Spec extends AnyFlatSpec {

  behavior of "Hello5"

  it should "append Hello5 output" in {
    Hello5.main(Array.empty)
    assert(new File("hello5.obtained").exists())

    Files.write(
      Paths.get("hello5.obtained"),
      "-tested".getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND
    )
  }
}