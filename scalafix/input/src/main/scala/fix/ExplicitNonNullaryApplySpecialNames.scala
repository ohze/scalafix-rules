/*
rule = ExplicitNonNullaryApply
*/
package fix

// ensure ExplicitNonNullaryApply don't add `()` to `???.asInstanceOf[Int]`
// note: we can define `class A { def asInstanceOf() = ??? }`
// but we can't call `(new A).asInstanceOf` (without `()`) so we don't need fix that case
object ExplicitNonNullaryApplySpecialNames {
  ???.asInstanceOf[Int]
  ???.isInstanceOf[String]

  def foo[T](c: () => T) = {
    val t = c.apply
    t.getClass
    t.toString
    t.hashCode
  }
}
