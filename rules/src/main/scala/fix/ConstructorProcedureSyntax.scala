package fix

import scalafix.v1._

import scala.meta._
import scala.meta.tokens.Token._

/** @see [[https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala ProcedureSyntax]] */
class ConstructorProcedureSyntax extends SyntacticRule("ConstructorProcedureSyntax") {
  override def description: String =
    "remove constructor procedure syntax `def this(..) {..}` => `def this(..) = {..}`"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      case t: Ctor.Secondary =>
        val initPosStart = t.init.pos.start
        // tokens from last RightParen before t.init until t.init
        val tokens = t.tokens
          .drop(5) // min 5 tokens: `def`, ` `, `this`, `(`, `)` before token `=`|`{`
          .takeWhile(_.pos.start < initPosStart)
          .reverse
          .takeWhile(!_.is[RightParen])
          .reverse
        tokens.find(_.is[Equals]) match {
          case Some(_) => Patch.empty
          case None =>
            val leftBrace = tokens.find(_.is[LeftBrace]).get // must exist
            Patch.addLeft(leftBrace, "= ")
        }
    }.asPatch
  }
}
