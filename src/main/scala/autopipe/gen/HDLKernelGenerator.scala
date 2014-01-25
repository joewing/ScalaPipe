package autopipe.gen

import autopipe._
import autopipe.opt.ASTOptimizer
import autopipe.opt.IROptimizer
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import scala.collection.mutable.HashMap

private[autopipe] class HDLKernelGenerator(
        _kt: InternalKernelType
    ) extends KernelGenerator(_kt) with HDLGenerator {

    private var stateCount      = 0
    private val optimizedAST    = ASTOptimizer(kt).optimize(kt.expression)
    private val ir              = IRNodeEmitter(kt).emit(optimizedAST)
    private val context         = new HDLIRContext(kt)
    protected val graph         = IROptimizer(kt, context).optimize(ir)
    private val moduleEmitter   = new HDLModuleEmitter(kt, graph)

    protected def emitFunctionHDL {
    }

    private def emitHDL: String = {

        val kernelName = "kernel_" + kt.name

        // Write the module header.
        write("module " + kernelName + "(")
        enter
        for (i <- kt.inputs) {
            write("input_" + i.name + ",")
            write("avail_" + i.name + ",")
            write("read_" + i.name + ",")
        }
        for (o <- kt.outputs) {
            write("output_" + o.name + ",")
            write("write_" + o.name + ",")
            write("afull_" + o.name + ",")
        }
        write("rst,")
        write("clk")
        leave
        write(");")
        enter

        write

        // I/O declarations.
        for (i <- kt.inputs) {
            val pts = getPortTypeString("input_" + i.name, i.valueType)
            write("input wire " + pts + ";")
            write("input wire avail_" + i.name + ";")
            write("output wire read_" + i.name + ";")
        }
        for (o <- kt.outputs) {
            val pts = getPortTypeString("output_" + o.name, o.valueType)
            write("output wire " + pts + ";")
            write("output wire write_" + o.name + ";")
            write("input wire afull_" + o.name + ";")
        }
        write("input wire rst;")
        write("input wire clk;")

        write

        // Configuration options.
        for (c <- kt.configs) {
            if (c.value != null) {
                write("parameter " + c.name + " = " + c.value + ";")
            } else {
                write("parameter " + c.name + " = 0;")
            }
        }
        if (!kt.configs.isEmpty) {
            write
        }

        // States.
        for (s <- kt.states) {
            emitLocal("state_" + s.name, s)
        }
        if (!kt.states.isEmpty) {
            write
        }

        // Temporaries.
        for (t <- kt.temps) {
            emitLocal("temp" + t.id, t)
        }
        if (!kt.temps.isEmpty) {
            write
        }

        // Internal state machine.
        write("reg [31:0] state;")
        write("reg [31:0] last_state;")
        write

        // Generate code.
        val nodeEmitter = new HDLKernelNodeEmitter(kt, graph, moduleEmitter)
        nodeEmitter.start
        graph.blocks.foreach(sb => nodeEmitter.emit(sb))
        nodeEmitter.stop

        // Write any required submodules.
        writeLeft(moduleEmitter.getOutput)
        write

        // Start the always block for the code.
        write("always @(posedge clk) begin")
        enter

        // Emit code.
        write(nodeEmitter)

        leave
        write("end")    // end always

        leave
        write("endmodule")

        emitFunctionHDL

        getOutput
    }

    override def emit(dir: File) {

        // Create the block directory.
        val parent = new File(dir, kt.name)
        parent.mkdir

        // Generate the HDL.
        val hdlFile = new File(parent, kt.name + ".v")
        val headerPS = new PrintStream(new FileOutputStream(hdlFile))
        headerPS.print(emitHDL)
        headerPS.close

    }

}
