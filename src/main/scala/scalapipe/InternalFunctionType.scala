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

    private[scalapipe] def returnType = outputs.size match {
        case 0 => ValueType.void
        case 1 => outputs.head.valueType
        case _ => sys.error("internal")
    }

    protected override def getGenerator: KernelGenerator = platform match {
        case Platforms.C        => new CFunctionGenerator(this)
        case Platforms.OpenCL   => new OpenCLFunctionGenerator(this)
        case Platforms.HDL      => new HDLFunctionGenerator(this)
        case _                  => sys.error("internal")
    }

}
