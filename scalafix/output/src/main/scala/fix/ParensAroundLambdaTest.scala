package fix

abstract class ParensAroundLambdaTest {
  Nil.foreach { (x: Nothing) => } // fix
  Nil.foreach { (x: Nothing) => } // keep
  val f: String => Unit = (s: String) => () // keep
  Map("a" -> 1).map { (x: (String, Int)) => // fix
    ???
  }

  Seq(1).map { (i: Int) => // fix
    i + 1
  }
  Seq(1).map { i => i + 1 } // keep

  Seq(1).foreach { implicit i: Int => } // keep
}
