/*
rule = ExplicitNonNullaryApply
*/
package fix

object StreamConverters {
  def fromJavaStream(stream: () => Int) = ???
  // StreamConvertersToJava from akka:
  def factory(): Int = 1
  fromJavaStream(factory)
}
