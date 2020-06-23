import _root_.scalafix.sbt.{BuildInfo => V}
import DottySupport._

inThisBuild(
  List(
    organization := "com.sandinh",
    homepage := Some(url("https://github.com/ohze/scalafix-rules")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
        Developer("thanhbv", "Bui Viet Thanh", "thanhbv@sandinh.net", url("https://sandinh.com"))),
    scalaVersion := V.scala213,
    crossScalaVersions := V.supportedScalaVersions,
    addCompilerPlugin(scalafixSemanticdb),
    scalacOptions ++= List("-Yrangepos", "-P:semanticdb:synthetics:on"),
    skip in publish := true,
  )
)

lazy val rules = project.settings(
  moduleName := "scalafix-rules",
  libraryDependencies += "ch.epfl.scala" %% "scalafix-rules" % V.scalafixVersion,
  skip in publish := false,
)

lazy val input = project

lazy val output = project

// This project is used to verify that output can be compiled with dotty
lazy val output3 = output.withId("output3").settings(
  target := (target.value / "../target-3").getCanonicalFile,
  crossScalaVersions := Seq("0.24.0", "0.25.0-RC2"),
  scalaVersion := crossScalaVersions.value.head,
  libraryDependencies := {
    val sv = scalaVersion.value
    libraryDependencies.value.map {
      case m if m.name == "semanticdb-scalac" => m.withDottyFullCompat(sv)
      case m => m
    }
  },
  scalacOptions --= List("-Yrangepos", "-P:semanticdb:synthetics:on"),
  Compile / sources := {
    val excludes = Set(
      "Any2StringAddTest", // use symbol literal
      "ConstructorProcedureSyntaxTest", // don't rewrite normal ProcedureSyntax
      "ExplicitNonNullaryApply", // `this id[String] ""` - expression expected but '[' found
      "ExplicitNonNullaryApplyInfix", // `lhs() shouldBe[String] arg()` - expression expected but '[' found
      "ExplicitNonNullaryApplyOver", // NullaryOverride
    )
    (Compile / sources).value.filterNot(f => excludes.contains(f.base))
  },
  test := {}
)

lazy val tests = project
  .settings(
    libraryDependencies += ("ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test).cross(CrossVersion.full),
    compile.in(Compile) :=
      compile.in(Compile).dependsOn(compile.in(input, Compile)).value,
    scalafixTestkitOutputSourceDirectories :=
      sourceDirectories.in(output, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      sourceDirectories.in(input, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(input, Compile).value)
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
