
package autopipe

import autopipe.dsl.AutoPipeFunction
import autopipe.gen.FunctionGenerator
import autopipe.gen.CFunctionGenerator
import autopipe.gen.HDLFunctionGenerator
import autopipe.gen.OpenCLFunctionGenerator
import java.io.File

private[autopipe] abstract class FunctionType(
        _ap: AutoPipe,
        _name: String,
        _symbols: SymbolTable,
        _platform: Platforms.Value,
        _loopBack: Boolean
    ) extends CodeObject(_ap, _name, _symbols, _platform, _loopBack) {

    private[autopipe] val args = symbols.inputs

    def this(ap: AutoPipe, apf: AutoPipeFunction, p: Platforms.Value) = {
        this(ap, apf.name, new SymbolTable(apf), p, apf.loopBack)
        apf.inputs.foreach(i => symbols.addInput(i.name, i.t.create()))
        symbols.addOutput("output", apf.returnType)
        apf.states.foreach { s =>
            symbols.addState(s.name, s.t, Literal.get(s.init, apf))
        }
        dependencies.add(apf.dependencies)
    }

    private[autopipe] def returnType = symbols.outputs.headOption match {
        case Some(os)  => os.valueType
        case None        => ValueType.void
    }

    def emit(dir: File)

    def internal: Boolean

}

private[autopipe] class ExternalFunctionType(
        ap: AutoPipe,
        apf: AutoPipeFunction,
        p: Platforms.Value
    ) extends FunctionType(ap, apf, p) {

    override def emit(dir: File) {
    }

    override def internal = false

}

private[autopipe] class InternalFunctionType(
        ap: AutoPipe,
        apf: AutoPipeFunction,
        p: Platforms.Value
    ) extends FunctionType(ap, apf, p) {

    private val root = apf.getRoot
    private val checked = TypeChecker.check(this, root)
    val expression = ConstantFolder.fold(this, checked)

    override def emit(dir: File) {
        val generator: FunctionGenerator = platform match {
            case Platforms.C        => new CFunctionGenerator(this)
            case Platforms.OpenCL   => new OpenCLFunctionGenerator(this)
            case Platforms.HDL      => new HDLFunctionGenerator(this)
            case _ => sys.error("internal")
        }
        generator.emit(dir)
    }

    override def functions = FunctionExtractor.functions(expression)

    override def objects = FunctionExtractor.objects(expression)

    override def internal = true

}
