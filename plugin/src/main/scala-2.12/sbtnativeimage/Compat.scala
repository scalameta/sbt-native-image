package sbtnativeimage

import scala.language.implicitConversions

import sbt.*

object Compat {
  implicit def richDef(o: Def.type): RichDef = new RichDef()
  class RichDef {
    def declareOutput(a: Any): Unit = ()
  }
  val compileInputs2 = Keys.compileInputs
}
