package fix

import scalafix.testkit.SemanticRuleSuite

class RuleSuite extends SemanticRuleSuite() {
//  testsToRun
//    .filter(_.path.input.toFile.getName.contains("2040.scala"))
//    .foreach(runOn)

  val (pendingTests, tests) = testsToRun.partition { t =>
    val fileName = t.path.input.toFile.getName
    fileName.endsWith("Pending.scala") || fileName.endsWith("2040.scala")
  }

  tests.foreach(runOn)
  pendingTests.foreach { t =>
    test(t.path.testName) {
      pendingUntilFixed(evaluateTestBody(t))
    }
  }
}
