package example

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets

object Hello3 {
  def main(args: Array[String]): Unit = {
    val cl = this.getClass.getClassLoader
    val c = cl.loadClass("example.Hello3")
    val h3 = c.getConstructor().newInstance()
    val text = h3.toString
    Files.write(
      Paths.get("hello3.obtained"),
      text.getBytes(StandardCharsets.UTF_8)
    )
  }
}

class Hello3 {
  override def toString: String = "Hello 3"
}
