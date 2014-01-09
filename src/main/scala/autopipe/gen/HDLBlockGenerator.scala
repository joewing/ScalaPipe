
package autopipe.gen

import autopipe._
import autopipe.opt.ASTOptimizer
import autopipe.opt.IROptimizer
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import scala.collection.mutable.HashMap

private[autopipe] class HDLBlockGenerator(_bt: InternalBlockType)
    extends BlockGenerator(_bt) with HDLGenerator {

    private var stateCount      = 0

    private val optimizedAST    = ASTOptimizer(bt).optimize(bt.expression)
    private val ir                 = IRNodeEmitter(bt).emit(optimizedAST)
    private val context          = new HDLIRContext(bt)
    private val graph             = IROptimizer(bt, context).optimize(ir)
    private val moduleEmitter  = new HDLModuleEmitter(bt, graph)

    private def emitHDL: String = {

        val moduleName = "X_" + bt.name

        // Write the module header.
        write("module " + moduleName + "(")
        enter
        for (i <- bt.inputs) {
            write("input_" + i.name + ",")
            write("avail_" + i.name + ",")
            write("read_" + i.name + ",")
        }
        for (o <- bt.outputs) {
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
        for (i <- bt.inputs) {
            val pts = getPortTypeString("input_" + i.name, i.valueType)
            write("input wire " + pts + ";")
            write("input wire avail_" + i.name + ";")
            write("output wire read_" + i.name + ";")
        }
        for (o <- bt.outputs) {
            val pts = getPortTypeString("output_" + o.name, o.valueType)
            write("output wire " + pts + ";")
            write("output wire write_" + o.name + ";")
            write("input wire afull_" + o.name + ";")
        }
        write("input wire rst;")
        write("input wire clk;")

        write

        // Configuration options.
        for (c <- bt.configs) {
            if (c.value != null) {
                write("parameter " + c.name + " = " + c.value + ";")
            } else {
                write("parameter " + c.name + " = 0;")
            }
        }
        if (!bt.configs.isEmpty) {
            write
        }

        // States.
        for (s <- bt.states) {
            emitLocal("state_" + s.name, s)
        }
        if (!bt.states.isEmpty) {
            write
        }

        // Temporaries.
        for (t <- bt.temps) {
            emitLocal("temp" + t.id, t)
        }
        if (!bt.temps.isEmpty) {
            write
        }

        // Internal state machine.
        write("reg [31:0] state;")
        write("reg [31:0] last_state;")
        write

        // Generate code.
        val nodeEmitter = new HDLBlockNodeEmitter(bt, graph, moduleEmitter)
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

        getOutput
    }

    override def emit(dir: File) {

        // Create the block directory.
        val basename = bt.name
        val subdirname = basename + "-dir"
        val parent = new File(dir, subdirname)
        parent.mkdir

        // Generate the HDL.
        val hdlFile = new File(parent, basename + ".v")
        val headerPS = new PrintStream(new FileOutputStream(hdlFile))
        headerPS.print(emitHDL)
        headerPS.close

        // Generate the makefile.
        val makeFile = new File(parent, "Makefile")
        val makePS = new PrintStream(new FileOutputStream(makeFile))
        makePS.println("V_FILES=" + basename + ".v")
        makePS.println("""
# Get the source files.
get_files:
	echo $(addprefix $(BLKDIR)/,$(C_FILES)) >> $(C_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(CXX_FILES)) >> $(CXX_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(VHDL_FILES)) >> $(VHDL_FILE_LIST)
	echo $(addprefix $(BLKDIR)/,$(V_FILES)) >> $(V_FILE_LIST)

# Generate the synthesis file.
synfile:
	$(foreach b,$(VHDL_FILES), \
		/bin/echo "add_file -vhdl \"$(BLKPATH)/$b\"" >> $(PRJFILE);)
	$(foreach b,$(V_FILES), \
		/bin/echo "add_file -verilog \"$(BLKPATH)/$b\"" >> $(PRJFILE);)

clean:
	rm -f *.o""")
        makePS.close

    }

}

