package scalapipe

import scalapipe.dsl.Func
import scalapipe.gen.KernelGenerator
import scalapipe.gen.CFunctionGenerator
import scalapipe.gen.HDLFunctionGenerator
import scalapipe.gen.OpenCLFunctionGenerator

private[scalapipe] class InternalFunctionType(
        sp: ScalaPipe,
        func: Func,
        p: Platforms.Value
    ) extends InternalKernelType(sp, func, p) {

    // Infer the return type if unspecified.
    if (outputs.size == 1 && outputs.head.valueType == ValueType.any) {
        inferReturnType
    }

    private[scalapipe] val returnType = outputs.size match {
        case 0 => ValueType.void
        case 1 => outputs.head.valueType
        case _ => Error.raise("multiple outputs not allowed for a Func",
                              expression); ValueType.any
    }

    protected override def getGenerator: KernelGenerator = platform match {
        case Platforms.C        => new CFunctionGenerator(this)
        case Platforms.OpenCL   => new OpenCLFunctionGenerator(this)
        case Platforms.HDL      => new HDLFunctionGenerator(this)
        case _                  => sys.error("internal")
    }

    private def inferReturnType {

        def getReturnTypes(node: ASTNode): Set[ValueType] = {
            node match {
                case rn: ASTReturnNode => Set(rn.a.valueType)
                case _ =>
                    val start = Set[ValueType]()
                    node.children.foldLeft(start) { (a, c) =>
                        a ++ getReturnTypes(c)
                    }
            }
        }

        val returnTypes = getReturnTypes(expression)
        if (!returnTypes.isEmpty) {
            val valueType = returnTypes.reduce { (a, b) =>
                TypeChecker.widestType(a, b)
            }
            symbols.setOutputType(valueType)
            val name = func.outputs.head.name
            func.outputs.clear
            func.outputs += new KernelOutput(name, valueType)
        }

    }


}
