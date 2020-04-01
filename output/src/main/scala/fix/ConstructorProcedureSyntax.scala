package fix

class ConstructorProcedureSyntax(i: Long) {
  def f/**/{
    println("f")
    class X{
      def this(j:Int)    = {this()}
      def this (j: String)= this(j.toInt)
    }
  }
  def this()= {this(1L)
    println("hi")
    class X{
      def this (j: String) = {this()}
    }
  }
  def this(i: Int) /**/
  //
  /***/
  = {
    this(i.toLong)
  }

  def this(i: String) = this(i.toLong)
}
