package autopipe.gen

import autopipe._
import autopipe.opt.ASTOptimizer
import autopipe.opt.IROptimizer

private[autopipe] class HDLFunctionGenerator(
        val ft: InternalFunctionType
    ) extends HDLKernelGenerator(ft) {

    private var stateCount = 0
    private val moduleEmitter = new HDLModuleEmitter(kt, graph)

    protected override def emitFunctionHDL {

        val functionName = ft.name

        // Write the module header.
        write("module " + functionName + "(")
        enter
        write("clk, start,")
        for (a <- ft.inputs) {
            write(emitSymbol(a) + ",")
        }
        write("result_out, ready_out);")
        write

        // Width configuration.  This is needed for compatability with
        // the built-in functions.
        write("parameter WIDTH = " + ft.returnType.bits + ";")
        write

        // I/O declarations.
        write("input wire clk;")
        write("input wire start;")
        for (i <- ft.inputs) {
            val pts = getTypeString(emitSymbol(i), i.valueType)
            write("input wire " + pts + ";")
        }
        val pts = getTypeString("result_out", ft.returnType)
        write("output reg " + pts + ";")
        write("output wire ready_out;")
        write

        // States.
        for (s <- ft.states) {
            emitLocal("state_" + s.name, s)
        }
        if (!ft.states.isEmpty) {
            write
        }

        // Temporaries.
        for (t <- ft.temps) {
            emitLocal("temp" + t.id, t)
        }
        if (!ft.temps.isEmpty) {
            write
        }

        // Internal state machine.
        write("reg [31:0] state;")
        write("reg [31:0] last_state;")
        write

        // Assign ready_out.
        write("assign ready_out = state == 0;")
        write

        // Generate code.
        val nodeEmitter = new HDLFunctionNodeEmitter(ft, graph, moduleEmitter)
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

    }

}
