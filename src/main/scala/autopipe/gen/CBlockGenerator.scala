
package autopipe.gen

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import java.io.File

import autopipe._
import autopipe.opt.IROptimizer

private[autopipe] class CBlockGenerator(_bt: InternalBlockType)
   extends BlockGenerator(_bt) with CLike with StateTrait with CTypeEmitter {

   private def emitHeader: String = {

      val bname = "ap_" + bt.name
      val sname = "struct " + bname + "_data"

      write
      write("#include \"X.h\"")
      for (i <- bt.dependencies.get(DependencySet.Include)) {
         write("#include <" + i + ">")
      }
      write

      bt.configs.foreach { c => emitType(c.valueType) }
      bt.states.foreach { s => emitType(s.valueType) }
      bt.inputs.foreach { i => emitType(i.valueType) }
      bt.outputs.foreach { o => emitType(o.valueType) }

      write
      write(sname + " {")
      enter
      for (c <- bt.configs) {
         val tname = c.valueType
         write(tname + " " + c.name + ";")
      }
      for (s <- bt.states if !s.isLocal) {
         val tname = s.valueType
         write(tname + " " + s.name + ";")
      }
      write("int ap_state_index;")
      if (bt.parameters.get('profile)) {
         write("unsigned long ap_clocks;")
      }
      if (bt.parameters.get('trace)) {
         write("FILE *trace_fd;")
      }
      leave
      write("};")

      write("void " + bname + "_init(" + sname + " *block);")
      write("void " + bname + "_destroy(" + sname + " *block);")
      write("void " + bname + "_push(" + sname + " *block,")
      write("   int port, void *ptr, int count);")
      write("int " + bname + "_go(" + sname + " *block);")
      write("void " + bname + "_push_signal(" + sname + " *block,")
      write("    int port, int type, int value);")

      getOutput

   }

   private def emitInit {
      write("void ap_" + bt.name + "_init(struct ap_" +
         bt.name + "_data *block)")
      write("{")
      enter
      if (bt.parameters.get('trace)) {
         write("char file_name[128];")
      }
      for (s <- bt.states if !s.isLocal) {
         val name = s.name
         val literal = s.value
         if (literal != null) {
            write("block->" + name + " = " + bt.getLiteral(literal) + ";")
         }
      }
      write("block->ap_state_index = 0;")
      if (bt.parameters.get('profile)) {
         // We initialize the clocks to one to account for the start state.
         write("block->ap_clocks = 1;")
      }
      if (bt.parameters.get('trace)) {
         write("sprintf(file_name, \"" + bt.name + "%d\", " +
               "ap_get_instance(block));")
         write("block->trace_fd = fopen(file_name, \"w\");")
      }
      leave
      write("}")
   }

   private def emitDestroy {
      write("void ap_" + bt.name + "_destroy(struct ap_" +
            bt.name + "_data *block)")
      write("{")
      enter
      if (bt.parameters.get('trace)) {
         write("fclose(block->trace_fd);")
      }
      leave
      write("}")
   }

   private def emitRun {

      val timing: Map[ASTNode, Int] =
         if (bt.expression.isPure && bt.parameters.get('profile)) {
            val ir             = IRNodeEmitter(bt).emit(bt.expression)
            val context        = new ProfileIRContext(bt)
            val graph          = IROptimizer(bt, context).optimize(ir)
            val moduleEmitter  = new HDLModuleEmitter(bt, graph)
            HDLTiming.computeAST(graph)
         } else null

      write("static int run(struct ap_" + bt.name + "_data *block)")
      write("{")
      enter

      // Declare locals.
      for (l <- bt.states if l.isLocal) {
         write(l.valueType + " " + l.name + ";")
      }

      // Declare outputs.
      for (o <- bt.outputs) {
         write(o.valueType + " *" + o.name + ";")
      }

      // Get all the inputs.
      for (i <- bt.inputs) {
         write(i.valueType + " *" + i.name + " = (" + i.valueType +
            "*)ap_get_input_data(block, " + i.id + ");")
      }

      // Generate the code to emit.
      currentState = 0
      val nodeEmitter = new CBlockNodeEmitter(bt, this, timing)
      nodeEmitter.emit(bt.expression)

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

      write("void ap_" + bt.name + "_push(struct ap_" +
         bt.name + "_data *block,")
      write("   int port, void *ptr, int count)")
      write("{")
      enter

      write("run(block);")

      leave
      write("}")

   }

   private def emitGo {

      write("int ap_" + bt.name + "_go(struct ap_" +
         bt.name + "_data *block)")
      write("{")
      enter

      if (requiresInput(bt.expression)) {
         write("return 1;")
      } else {
         write("return run(block);")
      }

      leave
      write("}")
   }

   private def emitPushSignal {
      write("void ap_" + bt.name + "_push_signal(struct ap_" +
         bt.name + "_data *block,")
      write("   int port, int type, int value)")
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
      getOutput
   }

   override def emit(dir: File) {

      import java.io.FileOutputStream
      import java.io.PrintStream

      // Create the block directory.
      val basename = bt.name
      val subdirname = basename + "-dir"
      val parent = new File(dir, subdirname)
      parent.mkdir

      // Generate the header
      val headerFile = new File(parent, basename + ".h")
      val headerPS = new PrintStream(new FileOutputStream(headerFile))
      headerPS.print(emitHeader)
      headerPS.close

      // Generate the source.
      val sourceFile = new File(parent, basename + ".c")
      val sourcePS = new PrintStream(new FileOutputStream(sourceFile))
      sourcePS.print("#include \"" + basename + ".h\"\n")
      sourcePS.print(emitSource)
      sourcePS.close

      // Generate the makefile.
      val makeFile = new File(parent, "Makefile")
      val makePS = new PrintStream(new FileOutputStream(makeFile))
      makePS.println("C_FILES=" + basename + ".c")
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

