/*
rule = ExplicitImplicitTypes
*/
package fix

import scala.concurrent.ExecutionContext

abstract class ExplicitImplicitTypesTest {
  trait E {
    def ec: ExecutionContext
  }
  trait T
  
  def f(e: E) = new T {
    private implicit def ec = e.ec
    final implicit val s1 = Seq(1)
    implicit var nil_ = Seq.empty[AnJavaInterface]
    implicit var i: Int = 10 // keep
    def g() = {
      implicit val l = 1L
      implicit var none = Option.empty[String]
    }
  }
}
