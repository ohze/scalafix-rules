# Scalafix rules
[![Build Statud](https://github.com/ohze/scalafix-rules/workflows/Scala%20CI/badge.svg)](https://github.com/ohze/scalafix-rules/actions?query=workflow%3A%22Scala+CI%22)

+ FinalObject
remove redundant `final` modifier for objects

+ ConstructorProcedureSyntax
remove constructor procedure syntax `def this(..) {..}` => `def this(..) = {..}`
This rule complement to the built-in [ProcedureSyntax](https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala) rule

+ ExplicitNonNullaryApply
Similar to [fix.scala213.ExplicitNonNullaryApply](https://github.com/scala/scala-rewrites/blob/1cea92d/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala) from scala-rewrites project
but don't need scala.meta.internal.pc.ScalafixGlobal to hook into scala-compiler.
So this rule can be run with scala 2.13.x

Don't like fix.scala213.ExplicitNonNullaryApply, this rule also add `()` methods
that is defined in java and be overridden in scala, eg the def in ExplicitNonNullaryApplyJavaPending test. 

+ Any2StringAdd
fix https://github.com/scala/scala-rewrites/issues/18
plus some improvements

+ NullaryOverride
Consistent nullary overriding, eg:
```scala
trait A {
  def i: Int
  def u(): Unit
}
trait B {
  def i() = 1 // fix by remove `()`: def i = 1
  def u = println("hi") // fix by add `()`: def u() = println("hi")
}
```

## Usage
+ eg, for ExplicitNonNullaryApply rule:
```
scalafix --rules=github:ohze/scalafix-rules/ExplicitNonNullaryApply
```
+ Note: to explicitly add type to implicit members (required by dotty) we can use the built-in ExplicitResultTypes rule
with config
```hocon
rules = [
ExplicitResultTypes
]
ExplicitResultTypes {
  memberVisibility = [] # only rewrite implicit members
  skipSimpleDefinitions = []
}
```

## Dev guide
```
cd scalafix
sbt ~tests/test
# edit scalafix/rules/src/main/scala/fix/*.scala
```
