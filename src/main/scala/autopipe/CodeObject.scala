
package autopipe

import autopipe.dsl.AutoPipeFunction
import autopipe.dsl.AutoPipeObject

private[autopipe] abstract class CodeObject(
        val ap: AutoPipe,
        val name: String,
        val symbols: SymbolTable,
        val platform: Platforms.Value,
        val loopBack: Boolean
    ) {

    private[autopipe] val configs = symbols.configs
    private[autopipe] val states = symbols.states
    private[autopipe] val temps = symbols.temps
    private[autopipe] val parameters = ap.parameters
    private[autopipe] val dependencies = new DependencySet

    private[autopipe] def getType(node: ASTSymbolNode): ValueType = {
        val name = node.symbol
        val vtype = symbols.getType(name)
        if (vtype == null) {
            Error.raise("symbol not declared: " + name, node)
            ValueType.void
        } else {
            vtype
        }
    }

    private[autopipe] def isInput(node: ASTSymbolNode): Boolean = {
        symbols.isInput(node.symbol)
    }

    private[autopipe] def isOutput(node: ASTSymbolNode): Boolean = {
        symbols.isOutput(node.symbol)
    }

    private[autopipe] def isInternal(f: AutoPipeFunction): Boolean = {
        f.isInternal(platform)
    }

    private[autopipe] def functions: Seq[AutoPipeFunction] = List()

    private[autopipe] def objects: Seq[AutoPipeObject] = List()

    private[autopipe] def createTemp(vt: ValueType): TempSymbol =
        symbols.createTemp(vt)

    private[autopipe] def releaseTemp(t: BaseSymbol) {
        symbols.releaseTemp(t)
    }

    private[autopipe] def getSymbol(name: String): BaseSymbol =
        symbols.get(name)

    private[autopipe] def getBaseOffset(name: String): Int =
        symbols.getBaseOffset(name)

    def getLiteral(lit: Literal): String = lit match {
        case sl: SymbolLiteral =>
            configs.find(_.name == sl.symbol) match {
                case Some(v)    => getLiteral(v.value)
                case None       =>
                    Error.raise("config option not found: " + sl.symbol, this)
            }
        case _ => lit.toString
    }

    override def toString = name

}

