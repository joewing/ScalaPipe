package scalapipe.gen

import java.io.File

import scalapipe._
import scalapipe.opt.IROptimizer

private[scalapipe] class CKernelGenerator(
        _kt: InternalKernelType
    ) extends KernelGenerator(_kt) with CGenerator with ASTUtils {

    protected def emitFunctionHeader {
    }

    protected def emitFunctionSource {
    }

    private def emitHeader: String = {

        val kname = s"sp_${kt.name}"
        val sname = s"struct ${kname}_data"

        write(s"#ifndef ${kname}_H_")
        write(s"#define ${kname}_H_")
        write("#include \"ScalaPipe.h\"")
        kt.dependencies.get(DependencySet.Include).foreach { i =>
            write(s"#include <$i>")
        }

        val typeEmitter = new CTypeEmitter
        kt.configs.foreach { c => typeEmitter.emit(c.valueType) }
        kt.states.foreach { s => typeEmitter.emit(s.valueType) }
        kt.inputs.foreach { i => typeEmitter.emit(i.valueType) }
        kt.outputs.foreach { o => typeEmitter.emit(o.valueType) }
        write(typeEmitter)

        write(s"$sname")
        enter
        kt.configs.foreach { c =>
            val cname = c.name
            val vtype = c.valueType
            write(s"$vtype $cname;")
        }
        kt.states.filter(!_.isLocal).foreach { s =>
            val sname = s.name
            val vtype = s.valueType
            write(s"$vtype $sname;")
        }
        if (kt.parameters.get('profile)) {
            write(s"unsigned long sp_clocks;")
        }
        if (kt.parameters.get('trace)) {
            write(s"FILE *trace_fd;")
        }
        leave
        write(";")

        write(s"void ${kname}_init($sname*);")
        write(s"void ${kname}_destroy($sname*);")
        write(s"void ${kname}_run($sname*);")
        emitFunctionHeader
        write(s"#endif")

        getOutput

    }

    private def emitInit {

        val kname = kt.name
        val sname = s"sp_${kname}_data"

        write(s"void sp_${kname}_init(struct $sname *kernel)")
        enter
        if (kt.parameters.get('trace)) {
            write(s"char file_name[128];")
        }
        for (s <- kt.states if !s.isLocal && s.value != null) {
            val field = s.name
            val value = kt.getLiteral(s.value)
            write(s"kernel->$field = $value;")
        }
        if (kt.parameters.get('profile)) {
            // We initialize the clocks to one to account for the start state.
            write(s"kernel->sp_clocks = 1;")
        }
        if (kt.parameters.get('trace)) {
            write(s"""sprintf(file_name, "$kname%d", """ +
                  s"""sp_get_instance(kernel));""")
            write(s"""kernel->trace_fd = fopen(file_name, "w");""")
        }
        leave
    }

    private def emitDestroy {
        val kname = kt.name
        write(s"void sp_${kname}_destroy(struct sp_${kname}_data *kernel)")
        enter
        if (kt.parameters.get('trace)) {
            write(s"fclose(kernel->trace_fd);")
        }
        leave
    }

    private def emitRun {

        val kname = kt.name
        val timing: Map[ASTNode, Int] =
            if (kt.expression.pure && kt.parameters.get('profile)) {
                val ir              = IRNodeEmitter(kt).emit(kt.expression)
                val context         = new ProfileIRContext(kt)
                val graph           = IROptimizer(kt, context).optimize(ir)
                val moduleEmitter   = new HDLModuleEmitter(kt, graph)
                HDLTiming.computeAST(graph)
            } else null

        // Create input functions.
        for (i <- kt.inputs) {
            val index = i.id
            val vtype = i.valueType
            val ktype = s"sp_${kname}_data"
            write(s"SP_READ_FUNCTION($vtype, $ktype, $index);")
        }

        write(s"void sp_${kname}_run(struct sp_${kname}_data *kernel)")
        enter

        // Declare locals.
        for (l <- kt.states if l.isLocal) {
            val name = l.name
            val vtype = l.valueType
            write(s"$vtype $name;")
        }

        // Declare outputs.
        for (o <- kt.outputs) {
            val name = o.name
            val vtype = o.valueType
            write(s"$vtype *$name;")
        }

        // Generate the code.
        val nodeEmitter = new CKernelNodeEmitter(kt, timing)
        nodeEmitter.emit(kt.expression)
        write(s"for(;;)")
        enter
        write(nodeEmitter)
        leave
        leave

    }

    private def emitSource: String = {
        emitInit
        emitDestroy
        emitRun
        emitFunctionSource
        getOutput
    }

    override def emit(dir: File) {

        import java.io.{FileOutputStream, PrintStream}

        // Create a directory for the kernel.
        val parent = new File(dir, kt.name)
        parent.mkdir

        // Generate the header
        val headerFile = new File(parent, s"${kt.name}.h")
        val headerPS = new PrintStream(new FileOutputStream(headerFile))
        headerPS.print(emitHeader)
        headerPS.close

        // Generate the source.
        val sourceFile = new File(parent, s"${kt.name}.c")
        val sourcePS = new PrintStream(new FileOutputStream(sourceFile))
        sourcePS.print("#include \"" + kt.name + ".h\"\n")
        sourcePS.print(emitSource)
        sourcePS.close

    }

}
