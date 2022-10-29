package example

import org.scalatest.flatspec.AnyFlatSpec
import java.nio.file.StandardOpenOption

class Hello4Spec extends AnyFlatSpec {

  behavior of "Hello4"

  it should "append Hello4 output" in {
    Hello4.main(Array.empty)
    assert(Paths.get("hello4.obtained").exists)
    Files.write(
      Paths.get("hello4.obtained"),
      text.getBytes(StandardCharsets.UTF_8),
      StandardOpenOption.APPEND
    )
    assert(true)
  }
}