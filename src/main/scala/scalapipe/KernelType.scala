package scalapipe

import scalapipe.dsl._

private[scalapipe] abstract class KernelType(
        val sp: ScalaPipe,
        val name: String,
        val symbols: SymbolTable,
        val platform: Platforms.Value
    ) {

    private[scalapipe] val configs = symbols.configs
    private[scalapipe] val states = symbols.states
    private[scalapipe] val temps = symbols.temps
    private[scalapipe] val parameters = new KernelParameters(sp.parameters)
    private[scalapipe] val dependencies = new DependencySet
    private[scalapipe] val label = LabelMaker.getTypeLabel
    private[scalapipe] val inputs = symbols.inputs
    private[scalapipe] val outputs = symbols.outputs

    def this(sp: ScalaPipe, kernel: Kernel, p: Platforms.Value) = {
        this(sp, kernel.name, new SymbolTable(kernel), p)
        kernel.inputs.foreach { i =>
            symbols.addInput(i.name, i.valueType)
        }
        kernel.outputs.foreach { o =>
            symbols.addOutput(o.name, o.valueType)
        }
        kernel.configs.foreach { c =>
            val lit = Literal.get(c.default, kernel)
            symbols.addConfig(c.name, c.valueType, lit)
        }
        kernel.states.foreach { s =>
            val lit = Literal.get(s.init, kernel)
            symbols.addState(s.name, s.valueType, lit)
        }
        dependencies.add(kernel.dependencies)
    }

    def pure: Boolean

    private[scalapipe] def addParameter(param: Symbol, value: Any) {
        parameters.set(param, value)
    }

    private[scalapipe] def ramDepth(vt: ValueType): Int = {
        val ramWidth = sp.parameters.get[Int]('memoryWidth)
        if (vt.flat) {
            return 0
        } else {
            return (vt.bits + ramWidth - 1) / ramWidth
        }
    }

    private[scalapipe] def ramDepth: Int = {
        val values = states ++ temps
        val valueDepth = values.map(v => ramDepth(v.valueType)).sum
        val funcDepth = functions.map { f =>
            sp.kernelType(f.name, platform).ramDepth
        }.sum
        valueDepth + funcDepth
    }

    private[scalapipe] def getType(node: ASTSymbolNode): ValueType = {
        val name = node.symbol
        val vtype = symbols.getType(name)
        if (vtype == null) {
            Error.raise("symbol not declared: " + name, node)
            ValueType.void
        } else {
            vtype
        }
    }

    private[scalapipe] def isInput(node: ASTSymbolNode): Boolean = {
        symbols.isInput(node.symbol)
    }

    private[scalapipe] def isOutput(node: ASTSymbolNode): Boolean = {
        symbols.isOutput(node.symbol)
    }

    private[scalapipe] def isInternal(f: Func): Boolean = {
        f.isInternal(platform)
    }

    private[scalapipe] def functions: Set[Func] = Set()

    private[scalapipe] def createTemp(vt: ValueType): TempSymbol =
        symbols.createTemp(vt)

    private[scalapipe] def releaseTemp(t: BaseSymbol) {
        symbols.releaseTemp(t)
    }

    private[scalapipe] def getSymbol(name: String): BaseSymbol =
        symbols.get(name)

    private[scalapipe] def getBaseOffset(name: String): Int =
        symbols.getBaseOffset(name)

    private[scalapipe] def getFuncOffset(name: String): Int = {
        val values = states ++ temps
        val valueDepth = values.map(v => ramDepth(v.valueType)).sum
        val preceding = functions.takeWhile(_.name != name).map { f =>
            sp.kernelType(f.name, platform).ramDepth
        }.sum
        valueDepth + preceding
    }

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

    def inputType(p: PortName): ValueType = {
        val s = symbols.getInput(p)
        if (s == null) {
            Error.raise("input port " + p + " not found", this)
        }
        s.valueType
    }

    def outputType(p: PortName): ValueType = {
        val s = symbols.getOutput(p)
        if (s == null) {
            Error.raise("output port " + p + " not found", this)
        }
        s.valueType
    }

    def inputType(n: String): ValueType = inputType(new StringPortName(n))

    def outputType(n: String): ValueType = outputType(new StringPortName(n))

    def inputIndex(p: PortName) = symbols.inputIndex(p)

    def outputIndex(p: PortName) = symbols.outputIndex(p)

    def inputIndex(n: String): Int = inputIndex(new StringPortName(n))

    def outputIndex(n: String): Int = outputIndex(new StringPortName(n))

    private[scalapipe] def emit(dir: java.io.File)

    private[scalapipe] def internal: Boolean

    private[scalapipe] def inputName(i: Int) = inputs(i).name

    private[scalapipe] def outputName(i: Int) = outputs(i).name

    def isInput(n: String) = inputs.exists(_.name == n)

    def isOutput(n: String) = outputs.exists(_.name == n)

    def isPort(n: String) = isInput(n) || isOutput(n)

    def isState(n: String) = states.exists(_.name == n)

    def isConfig(n: String) = configs.exists(_.name == n)

    def isLocal(n: String) = symbols.get(n) match {
        case sn: StateSymbol => sn.isLocal
        case _                    => false
    }

}
