package example

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object Hello2 {
  def main(args: Array[String]): Unit = {
    val text = List(1, 2, 3).toString() + "\n"
    Files.write(
      Paths.get("hello2.obtained"),
      text.getBytes(StandardCharsets.UTF_8)
    )
  }
}
