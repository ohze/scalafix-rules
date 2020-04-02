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
        val initTokens = t.init.tokens
        val initIdx = tokens.indexOf(initTokens.head)
        // last RightParen before init
        val lastRightParenIdx = tokens.lastIndexWhere(_.is[RightParen], initIdx - 1)
        // if slicedTokens don't have Equals token => need patching
        val slicedTokens = tokens.slice(lastRightParenIdx, initIdx - 1)
        slicedTokens.find(_.is[Equals]) match {
          case Some(_) => Patch.empty
          case None =>
            val lastRightParen = slicedTokens.head
            if (t.stats.nonEmpty) {
              Patch.addRight(lastRightParen, " =")
            } else {
              // remove Brace pair if stats.isEmpty (constructor `t` has only one init statement `this(..)`)
              val leftBraceIdx =
                lastRightParenIdx +
                slicedTokens.indexWhere(_.is[LeftBrace]) // must != -1

              val leftRemovingTokens = tokens.lineIfALlSpace(leftBraceIdx) match {
                case Nil =>
                  val spaces = tokens
                      .slice(lastRightParenIdx + 1, initIdx)
                      .indexWhere(_.is[Newline]) match {
                    // `def this(..)` spaces `{` spaces `this(..)..`
                    // remove:          ↑          ↑
                    // but keep one space
                    case -1 => tokens.spaces(lastRightParenIdx + 1, initIdx, leftBraceIdx) match {
                      case tks if tks.length > 1 => tks.tail
                      case tks => tks
                    }
                    // `def this(..)` spaces `{` spaces `\n`
                    // `  this(..)..`   ↑          ↑
                    // remove:          |          |
                    case i => tokens.spaces(
                      lastRightParenIdx + 1,
                      lastRightParenIdx + 1 + i,
                      leftBraceIdx
                    )
                  }
                  spaces :+ tokens(leftBraceIdx)

                case tks => tks
              }

              val rightBraceIdx = tokens.length - 1
              val rightRemovingTokens = tokens.lineIfALlSpace(rightBraceIdx) match {
                case Nil =>
                  // `def this(..) { this(..)` spaces `}`
                  // remove:                     ↑
                  tokens.spaces(
                    tokens.indexOf(initTokens.last) + 1,
                    rightBraceIdx
                  ) :+ tokens(rightBraceIdx)

                case tks => tks
              }

              Patch.addRight(lastRightParen, " =") +
                Patch.removeTokens(leftRemovingTokens ++ rightRemovingTokens)
            }
        }
    }.asPatch
  }
}

object ConstructorProcedureSyntax {
  implicit class TokensOps(val tokens: Tokens) extends AnyVal {
    def isSpaces: Boolean = tokens.forall(_.is[SpaceOrTab])
    def spaces(from: Int, until: Int, except: Int = -1): Seq[Token] =
      (tokens.slice(from, until), except) match {
        case (tks, -1) if tks.isSpaces => tks
        case (tks, _) =>
          val i = except - from
          (tks.take(i), tks.drop(i + 1)) match {
            case (l, r) if l.isSpaces && r.isSpaces => l ++ r
            case _ => Nil
          }
        case _ => Nil
      }
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
     * Else, return Nil */
    def lineIfALlSpace(i: Int): Seq[Token] = {
      tokens.lastIndexWhere(!_.is[SpaceOrTab], i - 1) match {
        case -1 => Nil
        case from if !tokens(from).is[Newline] => Nil
        case from => tokens.indexWhere(!_.is[SpaceOrTab], i + 1) match {
          case to if to != -1 && !tokens(to).is[Newline] => Nil
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
