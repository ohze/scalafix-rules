package fix

import scalafix.testkit.SemanticRuleSuite

class RuleSuite extends SemanticRuleSuite() {
  val (pendingTests, tests) = testsToRun.partition(_.path.input.toFile.getName.endsWith("Pending.scala"))
  tests.foreach(runOn)
  pendingTests.foreach { t =>
    test(t.path.testName) {
      pendingUntilFixed(evaluateTestBody(t))
    }
  }
}
