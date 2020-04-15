# Scalafix rules
[![Build Statud](https://github.com/ohze/scalafix-rules/workflows/Scala%20CI/badge.svg)](https://github.com/ohze/scalafix-rules/actions?query=workflow%3A%22Scala+CI%22)
+ FinalObject
remove redundant `final` modifier for objects
+ ConstructorProcedureSyntax
remove constructor procedure syntax `def this(..) {..}` => `def this(..) = {..}`
This rule complement to the built-in [ProcedureSyntax](https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala) rule
+ ExplicitNonNullaryApply
improve from scala/scala-rewrites#14
+ Any2StringAdd
fix https://github.com/scala/scala-rewrites/issues/18
plus some improvements
## Usage
+ ex, for FinalObject rule:
```
scalafix --rules=github:ohze/scalafix-rules/FinalObject
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
