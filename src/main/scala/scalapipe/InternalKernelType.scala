package scalapipe

import scalapipe.dsl.Kernel
import scalapipe.gen.KernelGenerator
import scalapipe.gen.CKernelGenerator
import scalapipe.gen.OpenCLKernelGenerator
import scalapipe.gen.HDLKernelGenerator
import java.io.File

private[scalapipe] class InternalKernelType(
        ap: AutoPipe,
        kernel: Kernel,
        p: Platforms.Value
    ) extends KernelType(ap, kernel, p) {

    private val root = kernel.getRoot
    private val checked = TypeChecker.check(this, root)
    val expression = ConstantFolder.fold(this, checked)

    override def internal = true

    protected def getGenerator: KernelGenerator = platform match {
        case Platforms.C        => new CKernelGenerator(this)
        case Platforms.OpenCL   => new OpenCLKernelGenerator(this)
        case Platforms.HDL      => new HDLKernelGenerator(this)
        case _                  => sys.error("internal")
    }

    override def emit(dir: File) {

        // Add dependencies from functions.
        functions.foreach { f =>
            if (!f.externals.contains(platform)) {
                dependencies.add(DependencySet.Include, f.name + ".h")
            }
        }

        // Extract locals.
        LocalExtractor.extract(this)

        getGenerator.emit(dir)

    }

    override def functions = FunctionExtractor.functions(expression)

}
