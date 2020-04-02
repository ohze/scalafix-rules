/*
rule = ConstructorProcedureSyntax
*/
package fix

class ConstructorProcedureSyntaxTest(i: Long) {
  def this(a: Int) { this(0L + 1) }
  def this(a: Int, b: Int) {
    this(0L)
  }
  def f/**/{
    println("f")
    class X{
      def this(a:Int)    { this() }
      def this (j: String)= this(j.toInt)
    }
  }
  def this(){this(1L)
    println("hi")
    class X{
      def this (j: String) // comment }
      {
        this()
      }
      def this (j: Int) { // comment {
        this()
      }
    }
    println(".")
  }
  def this(i: Float) /**/
  //
  /***/
  {
    this(i.toLong)
  }

  def this(i: String) = this(i.toLong)
}
