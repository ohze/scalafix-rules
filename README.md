# Scalafix rules
+ FinalObject
remove redundant `final` modifier for objects

## Usage
```
scalafix --rules=github:ohze/scalafix-rules/FinalObject
```

To develop rule:
```
sbt ~tests/test
# edit rules/src/main/scala/fix/*.scala
```
