package scalapipe.gen

import java.io.File

import scalapipe._
import scalapipe.opt.IROptimizer

private[scalapipe] class CKernelGenerator(
        _kt: InternalKernelType
    ) extends KernelGenerator(_kt) with CGenerator
      with StateTrait with ASTUtils {

    protected def emitFunctionHeader {
    }

    protected def emitFunctionSource {
    }

    private def emitHeader: String = {

        val kname = "ap_" + kt.name
        val sname = s"struct ${kname}_data"

        write(s"#ifndef ${kname}_H_")
        write(s"#define ${kname}_H_")
        write("#include \"X.h\"")
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
        write(s"int ap_state_index;")
        if (kt.parameters.get('profile)) {
            write(s"unsigned long ap_clocks;")
        }
        if (kt.parameters.get('trace)) {
            write(s"FILE *trace_fd;")
        }
        leave
        write(";")

        write(s"void ${kname}_init($sname*);")
        write(s"void ${kname}_destroy($sname*);")
        write(s"void ${kname}_push($sname*,int,void*,int);")
        write(s"int ${kname}_go($sname*);")
        emitFunctionHeader
        write(s"#endif")

        getOutput

    }

    private def emitInit {

        val kname = kt.name
        val sname = s"ap_${kname}_data"

        write(s"void ap_${kname}_init(struct $sname *block)")
        enter
        if (kt.parameters.get('trace)) {
            write(s"char file_name[128];")
        }
        for (s <- kt.states if !s.isLocal && s.value != null) {
            val field = s.name
            val value = kt.getLiteral(s.value)
            write(s"block->$field = $value;")
        }
        write(s"block->ap_state_index = 0;")
        if (kt.parameters.get('profile)) {
            // We initialize the clocks to one to account for the start state.
            write(s"block->ap_clocks = 1;")
        }
        if (kt.parameters.get('trace)) {
            write(s"""sprintf(file_name, "$kname%d", """ +
                  s"""ap_get_instance(block));""")
            write(s"""block->trace_fd = fopen(file_name, "w");""")
        }
        leave
    }

    private def emitDestroy {
        val kname = kt.name
        write(s"void ap_${kname}_destroy(struct ap_${kname}_data *block)")
        enter
        if (kt.parameters.get('trace)) {
            write(s"fclose(block->trace_fd);")
        }
        leave
    }

    private def emitRun {

        val timing: Map[ASTNode, Int] =
            if (kt.expression.pure && kt.parameters.get('profile)) {
                val ir                 = IRNodeEmitter(kt).emit(kt.expression)
                val context          = new ProfileIRContext(kt)
                val graph             = IROptimizer(kt, context).optimize(ir)
                val moduleEmitter  = new HDLModuleEmitter(kt, graph)
                HDLTiming.computeAST(graph)
            } else null

        val kname = kt.name
        write(s"static int run(struct ap_${kname}_data *block)")
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

        // Get all the inputs.
        for (i <- kt.inputs) {
            val name = i.name
            val vtype = i.valueType
            val id = i.id
            write(s"$vtype *$name = ($vtype*)ap_get_input_data(block, $id);")
        }

        // Generate the code to emit.
        currentState = 0
        val nodeEmitter = new CKernelNodeEmitter(kt, this, timing)
        nodeEmitter.emit(kt.expression)

        // Jump to the appropriate place to resume.
        writeSwitch("block->ap_state_index")
        for (i <- 1 to currentState) {
            write(s"case $i: goto AP_STATE_$i;")
        }
        write(s"case -1: return 1;")
        write(s"default: break;")
        writeEnd

        // Output the code.
        write(nodeEmitter)

        // Continue running by default.
        write(s"block->ap_state_index = 0;")
        writeReturn("0")

        leave

    }

    private def emitPush {

        val kname = kt.name
        write(s"void ap_${kname}_push(struct ap_${kname}_data *block,")
        write(s"    int port, void *ptr, int count)")
        enter
        write(s"run(block);")
        leave

    }

    private def emitGo {

        val kname = kt.name
        write(s"int ap_${kname}_go(struct ap_${kname}_data *block)")
        enter
        if (requiresInput(kt.expression)) {
            writeReturn("1")
        } else {
            writeReturn("run(block)")
        }
        leave
    }

    private def emitSource: String = {
        emitRun
        emitInit
        emitDestroy
        emitPush
        emitGo
        emitFunctionSource
        getOutput
    }

    override def emit(dir: File) {

        import java.io.{FileOutputStream, PrintStream}

        // Create a directory for the kernel.
        val parent = new File(dir, kt.name)
        parent.mkdir

        // Generate the header
        val headerFile = new File(parent, kt.name + ".h")
        val headerPS = new PrintStream(new FileOutputStream(headerFile))
        headerPS.print(emitHeader)
        headerPS.close

        // Generate the source.
        val sourceFile = new File(parent, kt.name + ".c")
        val sourcePS = new PrintStream(new FileOutputStream(sourceFile))
        sourcePS.print("#include \"" + kt.name + ".h\"\n")
        sourcePS.print(emitSource)
        sourcePS.close

    }

}
