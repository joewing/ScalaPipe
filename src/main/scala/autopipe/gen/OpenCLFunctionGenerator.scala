package autopipe.gen

import autopipe._
import java.io.File

private[autopipe] class OpenCLFunctionGenerator(
        _ft: InternalFunctionType
    ) extends OpenCLKernelGenerator(_ft) with CTypeEmitter {

    protected override def emitFunction {
        // TODO
    }

}
