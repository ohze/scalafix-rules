package fix

import scala.concurrent.ExecutionContext

abstract class ExplicitImplicitTypesTest {
  trait E {
    def ec: ExecutionContext
  }
  trait T
  
  def f(e: E) = new T {
    private implicit def ec: ExecutionContext = e.ec
    final implicit val s1: Seq[Int] = Seq(1)
    implicit var nil_ : Seq[AnJavaInterface] = Nil
    implicit var i: Int = 10 // keep
    def g() = {
      implicit val l: Long = 1L
      implicit var none: Option[String] = None
    }
  }
}
