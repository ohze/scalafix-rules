package fix

import scalafix.util.Newline
import scalafix.v1._

import scala.meta._
import scala.meta.classifiers.Classifier
import scala.meta.tokens.Token
import scala.meta.tokens.Token._

/** @see [[https://github.com/scalacenter/scalafix/blob/master/scalafix-rules/src/main/scala/scalafix/internal/rule/ProcedureSyntax.scala ProcedureSyntax]] */
class ConstructorProcedureSyntax extends SyntacticRule("ConstructorProcedureSyntax") {
  import ConstructorProcedureSyntax._

  override def description: String =
    "remove constructor procedure syntax `def this(..) {..}` => `def this(..) = ..`"
  override def isRewrite: Boolean = true

  override def fix(implicit doc: SyntacticDocument): Patch = {
    doc.tree.collect {
      // t like this: `def this(params)` ( `= this(..)` | `= { this(..).*}` | `{ this(..).*}` )
      case t: Ctor.Secondary =>
        val tokens = t.tokens
        val beforeInitIdx = tokens.indexOf(t.init.tokens.head) - 1
        // last RightParen before init
        val lastRightParenIdx = tokens.lastIndexWhere(_.is[RightParen], beforeInitIdx)
        // if slicedTokens don't have Equals token => need patching
        val slicedTokens = tokens.slice(lastRightParenIdx, beforeInitIdx)
        slicedTokens.find(_.is[Equals]) match {
          case Some(_) => Patch.empty
          case None =>
            val removes =
              if (t.stats.nonEmpty) Patch.empty
              else {
                // remove Brace pair if stats.isEmpty (constructor `t` has only one init statement `this(..)`)
                val leftBraceIdx =
                  lastRightParenIdx +
                  slicedTokens.indexWhere(_.is[LeftBrace]) // must != -1

                val rightBraceIdx = tokens.length - 1

                Patch.removeTokens(
                  tokens.lineIfALlSpace(leftBraceIdx) ++
                  tokens.lineIfALlSpace(rightBraceIdx)
                )
              }
            val lastRightParen = slicedTokens.head
            Patch.addRight(lastRightParen, " =") + removes
        }
    }.asPatch
  }
}

object ConstructorProcedureSyntax {
  implicit class TokensOps(val tokens: Tokens) extends AnyVal {
    /**
     * @return the following tokens:
     * {{{
     *            from
     *             ↓
     * def this(..)<new_line>
     * <spaces><tokens(i)><spaces><new_line>
     *                            ↑
     *                            to
     * }}}
     * If all tokens in the line of tokens(i) are Space|Tab
     * Else, return Seq(tokens(i)) */
    def lineIfALlSpace(i: Int): Seq[Token] = {
      def t = Seq(tokens(i))
      tokens.lastIndexWhere(!_.is[SpaceOrTab], i - 1) match {
        case -1 => t
        case from if !tokens(from).is[Newline] => t
        case from => tokens.indexWhere(!_.is[SpaceOrTab], i + 1) match {
          case to if to != -1 && !tokens(to).is[Newline] => t
          case to => tokens.slice(from, to.max(i + 1))
        }
      }
    }
  }
}

trait SpaceOrTab
object SpaceOrTab {
  def unapply(token: Token): Boolean = token.is[Space] || token.is[Tab]
  implicit def classifier[T <: Token]: Classifier[T, SpaceOrTab] = unapply
}
