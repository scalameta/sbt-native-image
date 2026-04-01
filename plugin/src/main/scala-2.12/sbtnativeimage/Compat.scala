package sbtnativeimage

import scala.language.implicitConversions

import sbt._

object Compat {
  implicit def richDef(o: Def.type): RichDef = new RichDef()
  class RichDef {
    def declareOutput(a: Any): Unit = ()
  }
  val compileInputs2 = Keys.compileInputs
}
