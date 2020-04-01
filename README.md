# Scalafix rules
+ FinalObject
remove redundant `final` modifier for objects
+ ConstructorProcedureSyntax
remove constructor procedure syntax `def this(..) {..}` => `def this(..) = {..}`
This rule complement to the built-in [ProcedureSyntax](https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala) rule

## Usage
```
scalafix --rules=github:ohze/scalafix-rules/FinalObject
```

## Dev guide
```
cd scalafix
sbt ~tests/test
# edit scalafix/rules/src/main/scala/fix/*.scala
```
