package fix

import scala.annotation.varargs
import scala.collection.immutable

final object VarargsHackTest {
  def create() = apply(settings = immutable.Seq(): _*)
  def create(a0: Int) = apply(settings = immutable.Seq(a0): _*)
  def create(a0: Int, a1: Int) = apply(settings = immutable.Seq(a0, a1): _*)
  def create(a0: Int, a1: Int, a2: Int) = apply(settings = immutable.Seq(a0, a1, a2): _*)
  def create(a0: Int, a1: Int, a2: Int, a3: Int) = apply(settings = immutable.Seq(a0, a1, a2, a3): _*)
  def create(a0: Int, a1: Int, a2: Int, a3: Int, a4: Int) = apply(settings = immutable.Seq(a0, a1, a2, a3, a4): _*)
  @varargs def create(a0: Int, a1: Int, a2: Int, a3: Int, a4: Int, settings: Int*) = apply(settings = immutable.Seq(a0, a1, a2, a3, a4) ++ settings: _*)

  def apply(settings: Int*) = ???
}
