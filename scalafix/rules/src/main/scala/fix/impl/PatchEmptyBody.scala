package fix.impl

import scalafix.patch.Patch
import scalafix.v1.{SemanticDocument, SymbolMatcher}

import scala.meta._

/** Patch rhs of, eg `val s = Seq.empty[Int]` to `val s: Seq[Int] = Nil` */
object PatchEmptyBody {
  private val option = SymbolMatcher.exact("scala/Option.")
  private val list = SymbolMatcher.exact(
    "scala/package.List.",
    "scala/collection/immutable/List."
  )
  private val seq = SymbolMatcher.exact(
    "scala/package.Seq.",
    "scala/collection/Seq.",
    "scala/collection/immutable/Seq."
  )

  def apply(term: Term)(implicit doc: SemanticDocument): Patch =
    term match {
      case q"${option(_)}.empty[$_]" =>
        Patch.replaceTree(term, "None")
      case q"${list(_)}.empty[$_]" =>
        Patch.replaceTree(term, "Nil")
      case q"${seq(_)}.empty[$_]" =>
        Patch.replaceTree(term, "Nil")
      case _ =>
        Patch.empty
    }
}
