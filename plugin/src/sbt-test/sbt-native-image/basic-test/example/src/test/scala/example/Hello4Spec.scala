package example

import org.scalatest.flatspec.AnyFlatSpec

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths, StandardOpenOption}

class Hello4Spec extends AnyFlatSpec {

  behavior of "Hello4"

  it should "append Hello4 output" in {
    Hello4.main(Array.empty)
    assert(new File("hello4.obtained").exists())

    Files.write(
      Paths.get("hello4.obtained"),
      "-tested".getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND
    )
  }
}
