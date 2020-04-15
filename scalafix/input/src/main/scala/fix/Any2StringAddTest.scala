/*
rule = Any2StringAdd
*/
package fix

object Any2StringAddTest {
  'a + 'b.toString()
  1 + "b$"
  (null: Any) + ""
  1f + ""
  1.toByte + ""
  (1 + 2) + ""
  (1 + 2).toDouble + ""

  class X {
    val i = 1
  }

  val x: X = null
  new X + "a"
  ({ new X }) + "a"
  (new X) + s"$x"
  (new X).i + ""
  x.i + "a"

  x + "a"
  x + " a"
  x + "$a"
  x + " a"
  x + " a" + x.i + 1 + /*hehe*/ 2 + null
}
