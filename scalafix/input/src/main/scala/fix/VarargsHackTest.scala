/*
rule = VarargsHack
*/
package fix

import scala.annotation.varargs

final object VarargsHackTest {
  @varargs
  def create(settings: Int*) = apply(settings = settings: _*)

  def apply(settings: Int*) = ???
}
