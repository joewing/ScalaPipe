
package autopipe.gen

import autopipe._
import autopipe.opt.IROptimizer
import java.io.File

private[autopipe] class CFunctionGenerator(
        _ft: InternalFunctionType
    ) extends FunctionGenerator(_ft) with CTypeEmitter {

    private def optional[T](cond: Boolean, value: T): Option[T] = {
        if (cond) Some(value) else None
    }

    private def emitHeader: String = {

        write
        write("#ifndef " + ft.name + "_H_")
        write("#define " + ft.name + "_H_")
        write
        write("#include \"X.h\"")
        for (i <- ft.dependencies.get(DependencySet.Include)) {
            write("#include <" + i + ">")
        }
        write

        ft.states.foreach { s => emitType(s.valueType) }
        ft.args.foreach { i => emitType(i.valueType) }
        emitType(ft.returnType)

        write
        val prof = optional(ft.parameters.get('profile), "unsigned long*")
        val args = prof.toList ++ ft.args.map(_.valueType.toString)
        val argString = args.mkString(", ")
        write(ft.returnType.toString + " " + ft.name + "(" + argString + ");")
        write

        write("#endif")
        write

        return getOutput

    }

    private def emitSource: String = {

        val timing: Map[ASTNode, Int] =
            if (ft.expression.isPure && ft.parameters.get('profile)) {
                val ir                 = IRNodeEmitter(ft).emit(ft.expression)
                val context          = new ProfileIRContext(ft)
                val graph             = IROptimizer(ft, context).optimize(ir)
                val moduleEmitter  = new HDLModuleEmitter(ft, graph)
                HDLTiming.computeAST(graph)
            } else null

        write
        write("#include \"" + ft.name + ".h\"")
        write

        val prof = optional(ft.parameters.get('profile), "unsigned long *clocks")
        val args = prof.toList ++ ft.args.map { a =>
            a.valueType.toString + " " + a.name
        }
        val argString = args.mkString(", ")
        write(ft.returnType.toString + " " + ft.name + "(" + argString + ")")
        write("{")
        enter

        // Declare locals.
        ft.states.foreach { l =>
            write(l.valueType + " " + l.name + ";")
        }
        write

        // Emit the code.
        val nodeEmitter = new CFunctionNodeEmitter(ft, timing)
        nodeEmitter.emit(ft.expression)
        write(nodeEmitter)
        write

        leave
        write("}")
        write

        return getOutput

    }

    override def emit(dir: File) {

        import java.io.FileOutputStream
        import java.io.PrintStream

        // Create a directory for the function.
        val basename = ft.name
        val subdirname = basename + "-dir"
        val parent = new File(dir, subdirname)
        parent.mkdir

        // Generate the header.
        val headerFile = new File(parent, basename + ".h")
        val headerPS = new PrintStream(new FileOutputStream(headerFile))
        headerPS.print(emitHeader)
        headerPS.close

        // Generate the source.
        val sourceFile = new File(parent, basename + ".c")
        val sourcePS = new PrintStream(new FileOutputStream(sourceFile))
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

