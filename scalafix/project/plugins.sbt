resolvers += Resolver.sonatypeRepo("releases")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.18-1")

addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.4.1")

addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.3")
