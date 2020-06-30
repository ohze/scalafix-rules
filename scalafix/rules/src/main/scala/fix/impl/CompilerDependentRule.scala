package fix.impl

import scalafix.internal.rule.CompilerException
import scalafix.internal.v1.LazyValue
import scalafix.v1._

import scala.meta.inputs.Input
import scala.meta.internal.pc.ScalafixGlobal
import scala.util.control.Exception.nonFatalCatch

abstract class CompilerDependentRule(global: LazyValue[ScalafixGlobal], name: String) extends SemanticRule(name) {
  override def fix(implicit doc: SemanticDocument): Patch = {
    try unsafeFix() catch {
      case e: CompilerException =>
        println(s"Retrying fix ${getClass.getSimpleName} because $e when fix ${path(doc.input)}")
        shutdownCompiler()
        global.restart()
        try unsafeFix() catch {
          case _: CompilerException =>
            Patch.empty /* ignore compiler crashes */
        }
    }
  }

  protected def unsafeFix()(implicit doc: SemanticDocument): Patch

  override def afterComplete(): Unit = shutdownCompiler()

  private def shutdownCompiler(): Unit = for (g <- global) nonFatalCatch { g.askShutdown(); g.close() }

  private def path(i: Input): String = i match {
    case Input.File(path, _) => path.toString()
    case Input.VirtualFile(path, _) => path
    case Input.Slice(i, _, _) => path(i)
    case i => i.getClass.getSimpleName
  }
}
