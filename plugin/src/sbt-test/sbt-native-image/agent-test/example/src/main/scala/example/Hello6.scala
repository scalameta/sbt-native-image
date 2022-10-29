package example

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object Hello6 {
  def main(args: Array[String]): Unit = {
    val cl = this.getClass.getClassLoader
    val c = cl.loadClass("example.Hello6")
    val h3 = c.getConstructor().newInstance()
    val text = h3.toString
    Files.write(
      Paths.get("hello6.obtained"),
      text.getBytes(StandardCharsets.UTF_8)
    )
  }
}

class Hello6 {
  override def toString: String = "Hello 6"
}
