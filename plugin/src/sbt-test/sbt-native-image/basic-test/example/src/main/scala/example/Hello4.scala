package example

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object Hello4 {
  def main(args: Array[String]): Unit = {
    val text = List(1, 2, 3, 4).toString()
    Files.write(
      Paths.get("hello4.obtained"),
      text.getBytes(StandardCharsets.UTF_8)
    )
  }
}
