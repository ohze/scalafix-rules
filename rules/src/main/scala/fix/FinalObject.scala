package fix

import scalafix.v1._
import scala.meta._
import scala.meta.tokens.Token._

class FinalObject extends SemanticRule("FinalObject") {
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree.collect {
      case t: Defn.Object if t.mods.exists(_.is[Mod.Final]) =>
        // remove `final` and the space after that keyword
        val tokens = t.tokens
        val i = tokens.indexWhere(_.is[KwFinal])
        Patch.removeTokens(tokens.slice(i, i + 2))
    }.asPatch
  }
}
