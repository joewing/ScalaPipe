
package autopipe.gen

import autopipe._
import autopipe.opt.ASTOptimizer
import autopipe.opt.IROptimizer
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import scala.collection.mutable.HashMap

private[autopipe] class HDLFunctionGenerator(_ft: InternalFunctionType)
   extends FunctionGenerator(_ft) with HDLGenerator {

   private var stateCount = 0
   private val optimizedAST   = ASTOptimizer(ft).optimize(ft.expression)
   private val ir             = IRNodeEmitter(ft).emit(optimizedAST)
   private val context        = new HDLIRContext(ft)
   private val graph          = IROptimizer(ft, context).optimize(ir)
   private val moduleEmitter  = new HDLModuleEmitter(ft, graph)

   private def emitHDL: String = {

      val moduleName = ft.name

      // Write the module header.
      write("module " + moduleName + "(")
      enter
      write("clk, start,")
      for (a <- ft.args) {
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
      for (i <- ft.args) {
         val pts = getPortTypeString(emitSymbol(i), i.valueType)
         write("input wire " + pts + ";")
      }
      val pts = getPortTypeString("result_out", ft.returnType)
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
      write("end")   // end always

      leave
      write("endmodule")

      getOutput
   }

   override def emit(dir: File) {

      // Create the directory.
      val basename = ft.name
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

