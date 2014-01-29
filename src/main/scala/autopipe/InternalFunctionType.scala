package autopipe

import autopipe.dsl.Func
import autopipe.gen.KernelGenerator
import autopipe.gen.CFunctionGenerator
import autopipe.gen.HDLFunctionGenerator
import autopipe.gen.OpenCLFunctionGenerator

private[autopipe] class InternalFunctionType(
        ap: AutoPipe,
        func: Func,
        p: Platforms.Value
    ) extends InternalKernelType(ap, func, p) {

    private[autopipe] def returnType = outputs.size match {
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
