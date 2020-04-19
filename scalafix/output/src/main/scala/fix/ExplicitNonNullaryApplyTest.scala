package fix

import scala.concurrent.duration.Duration

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

  trait Creator[T] {
    def create(): T
  }
  def baz[T](c: Creator[T]) = foo(c.create _)

  (null: Duration).isFinite
}
