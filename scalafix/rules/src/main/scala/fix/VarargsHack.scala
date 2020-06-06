package fix

import scalafix.v1._

import scala.meta._

/** Workaround https://github.com/lampepfl/dotty/issues/7212 */
class VarargsHack extends SyntacticRule("VarargsHack") {
  override def description: String = "lampepfl/dotty#7212"
  override def isRewrite: Boolean = true

  private def isVarargs(m: Mod) = m match {
    case Mod.Annot(Init(Type.Name("varargs"), _, _)) => true
    case _ => false
  }

  override def fix(implicit doc: SyntacticDocument): Patch =
    doc.tree.collect {
      case t: Defn.Def if t.mods.exists(isVarargs) =>
        val (args :+ last) +: paramss = t.paramss // last is the varargs param
        val Some(Type.Repeated(tpe)) = last.decltpe
        def newParamss(n: Int, includeVarargsParam: Boolean = false) = {
          val extraArgs = 0.until(n).map { i => last.copy(name = Name(s"a$i"), decltpe = Some(tpe)) }
          val varargsParam = if(includeVarargsParam) Seq(last) else Nil
          (args ++ extraArgs ++ varargsParam) +: paramss
        }
        def isAssign(name: Term.Name) = name.parent.exists {
          case Term.Assign(`name`, _) => true
          case _ => false
        }
        def argSeq(n: Int) = Term.Apply(
          Term.Select(Term.Name("immutable"), Term.Name("Seq")),
          0.until(n).map { i => Term.Name(s"a$i")}.toList
        )

        val marks = scala.collection.mutable.Set.empty[Tree]
        def newBody(n: Int, includeVarargsParam: Boolean = false) = t.body.transform {
          case name @ Term.Name(v) if v == last.name.value && !isAssign(name) && !name.parent.exists(marks.contains) =>
            if (!includeVarargsParam) argSeq(n)
            else {
              val ret = Term.ApplyInfix(argSeq(n), Term.Name("++"), Nil, List(name))
              marks += ret
              ret
            }
        }.asInstanceOf[Term]

        val newDefs = 0.to(5).map { n =>
          t.copy(
            mods = t.mods.filterNot(isVarargs),
            paramss = newParamss(n),
            body = newBody(n)
          )
        }

        val allDefs = newDefs :+ t.copy(
          paramss = newParamss(5, true),
          body = newBody(5, true)
        )

        val leadingSpaces = "\n" + " " * doc.tokenList.leadingSpaces(t.tokens.head).size

        Patch.addGlobalImport(importer"scala.collection.immutable") +
          Patch.replaceTree(t, allDefs.map(_.syntax).mkString(leadingSpaces))
    }.asPatch
}
