# Scalafix rules
[![Build Statud](https://github.com/ohze/scalafix-rules/workflows/Scala%20CI/badge.svg)](https://github.com/ohze/scalafix-rules/actions?query=workflow%3A%22Scala+CI%22)

This project contains scalafix rules I create when migrating [akka](https://github.com/akka/akka/) to dotty.

#### FinalObject
Remove redundant `final` modifier for objects:
```scala
final object Abc
```

#### ConstructorProcedureSyntax
Remove constructor procedure syntax: `def this(..) {..}` => `def this(..) = {..}`

This rule complement to the built-in [ProcedureSyntax](https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala) rule

#### ExplicitNonNullaryApply
Compare to [fix.scala213.ExplicitNonNullaryApply](https://github.com/scala/scala-rewrites/blob/1cea92d/rewrites/src/main/scala/fix/scala213/ExplicitNonNullaryApply.scala)
 from scala-rewrites project:
  - Don't need scala.meta.internal.pc.ScalafixGlobal to hook into scala-compiler.
    
    So this rule also add `()` to methods that is defined in java and be overridden in scala, eg the calls in [ExplicitNonNullaryApplyJavaPending](scalafix/input/src/main/scala/fix/scala213/ExplicitNonNullaryApplyJavaPending.scala) test. 
  - When I try scala-rewrites' ExplicitNonNullaryApply for akka sources, it crashed!
  - Don't need to publishLocal before use. [scala/scala-rewrites#32](https://github.com/scala/scala-rewrites/issues/32)
    
    [Just add](https://github.com/ohze/akka/blob/dotty/wip/.scalafix.conf) `"github:ohze/scalafix-rules/ExplicitNonNullaryApply"` to `.scalafix.conf`

#### Any2StringAdd
- ~~Fix https://github.com/scala/scala-rewrites/issues/18~~ (now fixed)
- Plus some improvements

#### NullaryOverride
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

#### ParensAroundLambda
Fix: parentheses are required around the parameter of a lambda
```scala
Seq(1).map { i: Int => // rewrite to: Seq(1).map { (i: Int) =>
 i + 1
}
Seq(1).map { i => i + 1 } // keep
```

#### ExplicitImplicitTypes
To explicitly add type to `implicit def/val/var`s (required by dotty) you need:
1. Use the built-in ExplicitResultTypes rule with config:
```hocon
rules = [
  ExplicitResultTypes
]
ExplicitResultTypes {
  memberVisibility = [] # only rewrite implicit members
  skipSimpleDefinitions = []
}
```
=> Add type to implicit members of `class`es, `trait`s

2. And this rule
```hocon
rules = [
  "github:ohze/scalafix-rules/ExplicitImplicitTypes"
]
# maybe need
ExplicitImplicitTypes.symbolReplacements {
  "scala/concurrent/ExecutionContextExecutor#" = "scala/concurrent/ExecutionContext#"
}
```
=> Add type to implicit local `def/val/var`s

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
