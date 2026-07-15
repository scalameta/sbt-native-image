package example

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object Hello1 {
  def main(args: Array[String]): Unit = {
    val text = List(1, 2, 3).toString() + "\n"
    Files.write(
      Paths.get("hello1.obtained"),
      text.getBytes(StandardCharsets.UTF_8)
    )
  }
}
