import _root_.scalafix.sbt.{BuildInfo => V}
import scala.collection.mutable.ListBuffer
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

lazy val mergeRules = taskKey[Seq[File]](
  "Merge rules that are implemented in multiple source files into a single .scala file\n" +
    "so scalafix users can use rule \"github:ohze/scalafix-rules/<rule name>\" in .scalafix.conf"
)

lazy val rules = project.settings(
  moduleName := "scalafix-rules",
  libraryDependencies += "ch.epfl.scala" %% "scalafix-rules" % V.scalafixVersion,
  skip in publish := false,
  Compile / sources := ((Compile / sources).value ++ mergeRules.value).distinct,
  mergeRules := {
    val srcDir = (Compile / scalaSource).value
    val log = streams.value.log

    val rulesToMerge = Seq(
      "ExplicitImplicitTypes",
      "ExplicitNonNullaryApply",
    )

    rulesToMerge.map { r =>
      val importPrefix = "import fix.impl."
      val imports = ListBuffer.empty[String]

      val bf = new StringBuilder(
        """// !! DO NOT EDIT !!
          |// This file is auto-generated by running sbt "rules / mergeRules"
          |""".stripMargin)

      IO.read(srcDir / "fix" / s"${r}Impl.scala").linesIterator.foreach {
        case s if s.startsWith(importPrefix) => imports += s.substring(importPrefix.length)
        case s => bf ++= s.replace(s"${r}Impl", r) += '\n'
      }

      imports.foreach { name =>
        val f = s"fix/impl/$name.scala"
        bf ++=
          s"// begin inlining $f\n" ++=
          IO.read(srcDir / f).stripPrefix("package fix.impl\n") ++=
          s"// end inlining $f\n"
      }

      val f = srcDir / "fix" / s"$r.scala"
      IO.write(f, bf.result())
      log.info(s"Merged $r.scala")
      f
    }
  },
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
  test := (Compile / compile).value
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
