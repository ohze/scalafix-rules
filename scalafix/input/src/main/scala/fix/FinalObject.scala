/*
rule = FinalObject
*/
package fix

final object FinalObject {
  // Add code that needs fixing here.
  final object A {
    def f = {
      final object A
      println()
    }
  }
  final case object B {
    val x = 1
    final case object A {}
    final class B
  }
  final class C {}
  final case class D(x: Int)
  final val x = 1
}
