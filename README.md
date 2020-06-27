# Scalafix rules
[![Build Statud](https://github.com/ohze/scalafix-rules/workflows/Scala%20CI/badge.svg)](https://github.com/ohze/scalafix-rules/actions?query=workflow%3A%22Scala+CI%22)

This project contains scalafix rules I create when migrating [akka](https://github.com/akka/akka/) to dotty.

#### ConstructorProcedureSyntax
Remove constructor procedure syntax: `def this(..) {..}` => `def this(..) = {..}`

This rule complement to the built-in [ProcedureSyntax](https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala) rule.

#### Any2StringAdd
- ~~Fix https://github.com/scala/scala-rewrites/issues/18~~ (now fixed)
- Plus some improvements

#### ParensAroundLambda
Fix: parentheses are required around the parameter of a lambda
```scala
Seq(1).map { i: Int => // rewrite to: Seq(1).map { (i: Int) =>
 i + 1
}
Seq(1).map { i => i + 1 } // keep
```

#### FinalObject
Remove redundant `final` modifier for objects:
```scala
final object Abc
```

#### NullaryOverride
Consistent nullary overriding
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

#### ExplicitNonNullaryApply
Compare to [fix.scala213.ExplicitNonNullaryApply](https://github.com/scala/scala-rewrites/blob/1cea92d/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala)
 from scala-rewrites project:
+ Pros
  - When I try scala-rewrites' ExplicitNonNullaryApply for akka sources, it crashed!
  - This rule run faster than scala-rewrites' one.
  - Don't need to publishLocal to use. [scala/scala-rewrites#32](https://github.com/scala/scala-rewrites/issues/32)
  
    [Just add](https://github.com/ohze/akka/blob/dotty/wip/.scalafix.conf) `"github:ohze/scalafix-rules/ExplicitNonNullaryApply"` to `.scalafix.conf`
+ Cons    
  - This rule also add `()` to methods that are defined in java and be overridden in scala,
    eg the calls in [ExplicitNonNullaryApplyJavaPending](scalafix/input/src/main/scala/fix/scala213/ExplicitNonNullaryApplyJavaPending.scala) test.
    Those `()`s are not required by dotty.
+ Technical note: This rule don't need `scala.meta.internal.pc.ScalafixGlobal` to hook into scala-compiler.

#### ExplicitImplicitTypes
Before scalacenter/scalafix#1180 is fixed, to explicitly add type to `implicit def/val/var`s (required by dotty) you need:
+ Use the built-in [ExplicitResultTypes](https://scalacenter.github.io/scalafix/docs/rules/ExplicitResultTypes.html) rule with config:
```hocon
rules = [
  ExplicitResultTypes
]
ExplicitResultTypes {
  memberVisibility = [] # only rewrite implicit members
}
```
=> Add type to implicit members of `class`es, `trait`s
```scala
trait T {
  // rewrite to `implicit val s: Seq[String] = Nil`
  implicit val s = Seq.empty[String]
  // rewrite to `implicit def i: Int = 1`
  implicit def i = 1
}
```

+ *And* use this rule
```hocon
rules = [
  "github:ohze/scalafix-rules/ExplicitImplicitTypes"
]
# Optinal
ExplicitImplicitTypes.symbolReplacements {
  "scala/concurrent/ExecutionContextExecutor#" = "scala/concurrent/ExecutionContext#"
}
```
=> Add type to implicit local `def/val/var`s that its parent is a trait/class
```scala
import scala.concurrent.ExecutionContextExecutor

trait T
trait A {
  def f() = {
    class C {
      // rewrite to `implicit val i: Int = 1`
      implicit val i = 1
    }
    ???
  }
  def someEc(): ExecutionContextExecutor
  def g() = new T {
    // rewrite to `implicit def ec: ExecutionContext = someEc()`
    implicit def ec = someEc()
  }
}
```

## Usage
+ Eg, for ExplicitNonNullaryApply rule:
```
scalafix --rules=github:ohze/scalafix-rules/ExplicitNonNullaryApply
```

## Dev guide
```
cd scalafix
sbt ~test
# edit scalafix/rules/src/main/scala/fix/*.scala
```

## Licence
This software is licensed under the Apache 2 license:
http://www.apache.org/licenses/LICENSE-2.0

Copyright 2020 Sân Đình (https://sandinh.com)
