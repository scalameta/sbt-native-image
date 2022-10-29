package example

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

class Hello6Spec extends AnyFlatSpec {

  behavior of "Hello6"

  it should "append Hello6 output" in {
    Hello6.main(Array.empty)
    assert(new File("Hello6.obtained").exists())

    Files.write(
      Paths.get("Hello6.obtained"),
      "-tested".getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND
    )
  }
}