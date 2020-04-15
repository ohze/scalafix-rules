package fix

object Any2StringAddTest {
  String.valueOf('a) + 'b.toString()
  "" + 1 + "b$"
  String.valueOf(null: Any) + ""
  "" + 1f + ""
  "" + 1.toByte + ""
  "" + (1 + 2) + ""
  "" + (1 + 2).toDouble + ""

  class X {
    val i = 1
  }

  val x: X = null
  String.valueOf(new X) + "a"
  String.valueOf({ new X }) + "a"
  String.valueOf(new X) + s"$x"
  "" + (new X).i + ""
  "" + x.i + "a"

  String.valueOf(x) + "a"
  String.valueOf(x) + " a"
  String.valueOf(x) + "$a"
  String.valueOf(x) + " a"
  String.valueOf(x) + " a" + x.i + 1 + /*hehe*/ 2 + null
}
