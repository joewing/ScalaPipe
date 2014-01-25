package autopipe.gen

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import java.io.File

import autopipe._
import autopipe.opt.IROptimizer

private[autopipe] class CKernelGenerator(
        _kt: InternalKernelType
    ) extends KernelGenerator(_kt)
    with CLike with StateTrait with CTypeEmitter {

    protected def emitFunctionHeader {
    }

    protected def emitFunctionSource {
    }

    private def emitHeader: String = {

        val bname = "ap_" + kt.name
        val sname = "struct " + bname + "_data"

        write
        write("#ifndef " + kt.name + "_H_")
        write("#define " + kt.name + "_H_")
        write
        write("#include \"X.h\"")
        for (i <- kt.dependencies.get(DependencySet.Include)) {
            write("#include <" + i + ">")
        }
        write

        kt.configs.foreach { c => emitType(c.valueType) }
        kt.states.foreach { s => emitType(s.valueType) }
        kt.inputs.foreach { i => emitType(i.valueType) }
        kt.outputs.foreach { o => emitType(o.valueType) }

        write
        write(sname + " {")
        enter
        for (c <- kt.configs) {
            val tname = c.valueType
            write(tname + " " + c.name + ";")
        }
        for (s <- kt.states if !s.isLocal) {
            val tname = s.valueType
            write(tname + " " + s.name + ";")
        }
        write("int ap_state_index;")
        if (kt.parameters.get('profile)) {
            write("unsigned long ap_clocks;")
        }
        if (kt.parameters.get('trace)) {
            write("FILE *trace_fd;")
        }
        leave
        write("};")

        write("void " + bname + "_init(" + sname + " *block);")
        write("void " + bname + "_destroy(" + sname + " *block);")
        write("void " + bname + "_push(" + sname + " *block,")
        write("    int port, void *ptr, int count);")
        write("int " + bname + "_go(" + sname + " *block);")
        write("void " + bname + "_push_signal(" + sname + " *block,")
        write("     int port, int type, int value);")

        emitFunctionHeader

        write("#endif")
        write

        getOutput

    }

    private def emitInit {
        write("void ap_" + kt.name + "_init(struct ap_" +
            kt.name + "_data *block)")
        write("{")
        enter
        if (kt.parameters.get('trace)) {
            write("char file_name[128];")
        }
        for (s <- kt.states if !s.isLocal) {
            val name = s.name
            val literal = s.value
            if (literal != null) {
                write("block->" + name + " = " + kt.getLiteral(literal) + ";")
            }
        }
        write("block->ap_state_index = 0;")
        if (kt.parameters.get('profile)) {
            // We initialize the clocks to one to account for the start state.
            write("block->ap_clocks = 1;")
        }
        if (kt.parameters.get('trace)) {
            write("sprintf(file_name, \"" + kt.name + "%d\", " +
                    "ap_get_instance(block));")
            write("block->trace_fd = fopen(file_name, \"w\");")
        }
        leave
        write("}")
    }

    private def emitDestroy {
        write("void ap_" + kt.name + "_destroy(struct ap_" +
                kt.name + "_data *block)")
        write("{")
        enter
        if (kt.parameters.get('trace)) {
            write("fclose(block->trace_fd);")
        }
        leave
        write("}")
    }

    private def emitRun {

        val timing: Map[ASTNode, Int] =
            if (kt.expression.isPure && kt.parameters.get('profile)) {
                val ir                 = IRNodeEmitter(kt).emit(kt.expression)
                val context          = new ProfileIRContext(kt)
                val graph             = IROptimizer(kt, context).optimize(ir)
                val moduleEmitter  = new HDLModuleEmitter(kt, graph)
                HDLTiming.computeAST(graph)
            } else null

        write("static int run(struct ap_" + kt.name + "_data *block)")
        write("{")
        enter

        // Declare locals.
        for (l <- kt.states if l.isLocal) {
            write(l.valueType + " " + l.name + ";")
        }

        // Declare outputs.
        for (o <- kt.outputs) {
            write(o.valueType + " *" + o.name + ";")
        }

        // Get all the inputs.
        for (i <- kt.inputs) {
            write(i.valueType + " *" + i.name + " = (" + i.valueType +
                "*)ap_get_input_data(block, " + i.id + ");")
        }

        // Generate the code to emit.
        currentState = 0
        val nodeEmitter = new CKernelNodeEmitter(kt, this, timing)
        nodeEmitter.emit(kt.expression)

        // Jump to the appropriate place to resume.
        write("switch(block->ap_state_index) {")
        for (i <- 1 to currentState) {
            write("case " + i + ": goto AP_STATE_" + i + ";")
        }
        write("case -1: return 1;")
        write("default: break;")
        write("}")

        // Output the code.
        write(nodeEmitter)

        // Continue running by default.
        write("block->ap_state_index = 0;")
        write("return 0;")

        leave
        write("}")

    }

    private def emitPush {

        write("void ap_" + kt.name + "_push(struct ap_" +
            kt.name + "_data *block,")
        write("    int port, void *ptr, int count)")
        write("{")
        enter

        write("run(block);")

        leave
        write("}")

    }

    private def emitGo {

        write("int ap_" + kt.name + "_go(struct ap_" +
            kt.name + "_data *block)")
        write("{")
        enter

        if (requiresInput(kt.expression)) {
            write("return 1;")
        } else {
            write("return run(block);")
        }

        leave
        write("}")
    }

    private def emitPushSignal {
        write("void ap_" + kt.name + "_push_signal(struct ap_" +
            kt.name + "_data *block,")
        write("    int port, int type, int value)")
        write("{")
        enter
        leave
        write("}")
    }

    private def emitSource: String = {
        emitRun
        emitInit
        emitDestroy
        emitPush
        emitGo
        emitPushSignal
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

