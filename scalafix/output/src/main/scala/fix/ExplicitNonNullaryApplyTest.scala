package fix

import scala.concurrent.duration.Duration
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

// ensure ExplicitNonNullaryApply don't add `()` to `???.asInstanceOf[Int]`
// note: we can define `class A { def asInstanceOf() = ??? }`
// but we can't call `(new A).asInstanceOf` (without `()`) so we don't need fix that case
abstract class ExplicitNonNullaryApplyTest {
  ???.asInstanceOf[Int]
  ???.isInstanceOf[String]

  def foo[T](c: () => T) = {
    val t = c.apply()
    t.##

    t.getClass
    t.toString
    t.hashCode

    t.getClass()
    t.toString()
    t.hashCode()
  }
  class C {
    def f = clone
    def f2 = clone()
  }
  def c: C
  c.##
  c.getClass
  c.toString
  c.hashCode
  c.getClass()
  c.toString()
  c.hashCode()

  trait Creator[T] {
    def create(): T
  }
  def baz[T](c: Creator[T]) = foo(c.create _)

  (null: Duration).isFinite

  def createInstanceFor[T: ClassTag]: Try[T]
  // akka.coordination.lease.scaladsl.LeaseProvider#loadLease
  def loadLease[T: ClassTag]: Try[T] = createInstanceFor[T] match {
    case s: Success[T] => s
    case f: Failure[_] => f
  }

  override def toString: String = ""
  def toStringTest = this.toString

  class ActorSystem
  def await0(implicit system: ActorSystem) = ???
  def await1(system: ActorSystem) = ???
  def await()(implicit system: ActorSystem) = ???
  implicit val system: ActorSystem = ???
  this.await0
  this.await1 _
  this.await() //fix
  this.await()
}
