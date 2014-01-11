
package autopipe

import autopipe.dsl.AutoPipeBlock
import autopipe.gen.BlockGenerator
import autopipe.gen.CBlockGenerator
import autopipe.gen.OpenCLBlockGenerator
import autopipe.gen.HDLBlockGenerator
import java.io.File

private[autopipe] class InternalBlockType(ap: AutoPipe,
                                          apb: AutoPipeBlock,
                                          p: Platforms.Value)
        extends BlockType(ap, apb, p) {

    private val root = apb.getRoot
    private val checked = TypeChecker.check(this, root)
    val expression = ConstantFolder.fold(this, checked)

    override def internal = true

    override def emit(dir: File) {

        // Add dependencies from functions.
        functions.foreach { f =>
            if (!f.externals.contains(platform)) {
                dependencies.add(DependencySet.Include, f.name + ".h")
            }
        }

        // Extract locals.
        LocalExtractor.extract(this)

        val generator: BlockGenerator = platform match {
            case Platforms.C          => new CBlockGenerator(this)
            case Platforms.OpenCL    => new OpenCLBlockGenerator(this)
            case Platforms.HDL        => new HDLBlockGenerator(this)
            case _ => Error.raise("no block generator for " + platform)
        }
        generator.emit(dir)

    }

    override def functions = FunctionExtractor.functions(expression)

    override def objects = FunctionExtractor.objects(expression)

}
