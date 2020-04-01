package fix

object FinalObject {
  // Add code that needs fixing here.
  object A {
    def f = {
      object A
      println()
    }
  }
  case object B {
    val x = 1
    case object A {}
    final class B
  }
  final class C {}
  final case class D(x: Int)
  final val x = 1
}
