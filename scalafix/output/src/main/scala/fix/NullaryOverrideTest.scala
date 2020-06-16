package fix

import scala.annotation.unchecked.uncheckedVariance

trait ActorRef[-T] {
  def unsafeUpcast[U >: T @uncheckedVariance]: ActorRef[U]
}

trait ActorRefImpl[-T] extends ActorRef[T] {
  // must not add `()` to `unsafeUpcast()[U...`
  final override def unsafeUpcast[U >: T @uncheckedVariance]: ActorRef[U] = this.asInstanceOf[ActorRef[U]]
}

abstract class NullaryOverrideTest {
  class I1[T] extends Iterator[T] {
    def next(): T = ???
    def hasNext: Boolean = ???
    def foo(): T = ???
  }

  class I2[T] extends Iterator[T] {
    def next(): T = ???
    def hasNext: Boolean = ???
    def foo: T = ???
  }
}
