
package autopipe.gen

import scala.collection.mutable.HashSet
import java.io.File

import autopipe._

private[autopipe] class OpenCLBlockGenerator(_bt: InternalBlockType)
    extends BlockGenerator(_bt) with StateTrait {

    private def clwrite(s: String) {
        write("\"" + s + "\\n\"")
    }

    private def clwrite() {
        clwrite("")
    }

    private def clwrite(gen: Generator) {
        for (s <- gen.getOutput.split("\n")) {
            clwrite(s)
        }
    }

    private def getOpenCLType(t: ValueType): String = {
        t.baseType match {
            case ValueType.unsigned8    => "uchar"
            case ValueType.signed8      => "char"
            case ValueType.unsigned16  => "ushort"
            case ValueType.signed16     => "short"
            case ValueType.unsigned32  => "uint"
            case ValueType.signed32     => "int"
            case ValueType.unsigned64  => "ulong"
            case ValueType.signed64     => "long"
            case ValueType.float32      => "float"
            case ValueType.float64      => "double"
            case a: ArrayValueType      =>
                getOpenCLType(a.itemType) + "[" + a.length + "]"
            case _                            =>
                Error.raise("Type " + t + " not supported for OpenCL blocks")
        }
    }

    private def emitType(t: ValueType, emittedTypes: HashSet[ValueType]) {
        if (!emittedTypes.contains(t)) {
            emittedTypes += t
            t.dependencies.foreach { d => emitType(d, emittedTypes) }
            clwrite("#ifndef DECLARED_" + t.name)
            clwrite("#define DECLARED_" + t.name)
            t match {
                case at: ArrayValueType =>
                    clwrite("typedef struct {")
                    clwrite(getOpenCLType(at.itemType) +
                              " values[" + at.length + "];")
                    clwrite("} " + at.name + ";")
                case td: TypeDefValueType =>
                    clwrite("typedef " + td.value + " " + td.name + ";")
                case ft: FixedValueType =>
                    clwrite("typedef " + getOpenCLType(ft.baseType) +
                              " " + ft.name + ";")
                case _ =>
                    clwrite("typedef " + getOpenCLType(t) + " " +
                              t.baseType.name + ";")
            }
            clwrite("#endif")
        }
    }

    private def emitSource: String = {

        write("static const char *" + bt.name + "_source[] = {")

        clwrite("#line 3 \\\"" + bt.name + ".cl\\\"")
        clwrite("#pragma OPENCL EXTENSION cl_khr_byte_addressable_store: enable")
        clwrite("#pragma OPENCL EXTENSION cl_khr_fp64: enable")

        // Write typedefs.
        val emittedTypes = new HashSet[ValueType]
        bt.configs.foreach { c => emitType(c.valueType, emittedTypes) }
        bt.states.foreach { s => emitType(s.valueType, emittedTypes) }
        bt.inputs.foreach { i => emitType(i.valueType, emittedTypes) }
        bt.outputs.foreach { o => emitType(o.valueType, emittedTypes) }
        emitType(ValueType.signed8, emittedTypes)    // Booleans
        emitType(ValueType.signed32, emittedTypes)  // Integer literals
        emitType(ValueType.float64, emittedTypes)    // Float literals

        // Write the block control struct.
        clwrite("typedef struct {")
        clwrite("int ap_ready;")
        clwrite("int ap_state_index;")
        for (i <- bt.inputs) {
            clwrite("int " + i.name + "_size;")
            clwrite("int " + i.name + "_read;")
        }
        for (o <- bt.outputs) {
            clwrite("int " + o.name + "_size;")
            clwrite("int " + o.name + "_sent;")
        }
        clwrite("} " + bt.name + "_control;")

        // Write the block data struct.
        clwrite("typedef struct {")
        for (s <- bt.states if !s.isLocal) {
            clwrite(s.valueType.name + " " + s.name + ";")
        }
        clwrite("} " + bt.name + "_data;")

        // Start the kernel function.
        clwrite("__kernel void " + bt.name +
                  "(__global " + bt.name + "_control *control, " +
                  "__global " + bt.name + "_data *block")

        // Input ports.
        for (i <- bt.inputs) {
            clwrite(", __global " + i.valueType + " *" + i.name)
        }

        // Output ports.
        for (o <- bt.outputs) {
            clwrite(", __global " + o.valueType + " *" + o.name)
        }

        clwrite(") {")

        // Declare locals.
        for (l <- bt.states if l.isLocal) {
            clwrite(l.valueType.name + " " + l.name + ";")
        }

        // Generate the kernel code.
        val nodeEmitter = new OpenCLBlockNodeEmitter(bt, this, null)
        nodeEmitter.emit(bt.expression)

        clwrite("for(;;) {")

        clwrite("const int id = get_local_id(0);")
        if (bt.states.filter { s => !s.isLocal }.isEmpty) {
            clwrite("int unit_size = get_local_size(0);")
            for (i <- bt.inputs) {
                clwrite("unit_size = min(unit_size, " +
                          "control->" + i.name + "_size - " +
                          "control->" + i.name + "_read);")
            }
            for (i <- bt.outputs) {
                clwrite("unit_size = min(unit_size, " +
                          "control->" + i.name + "_size - " +
                          "control->" + i.name + "_sent);")
            }
            clwrite("unit_size = max(unit_size, 1);")
        } else {
            clwrite("const int unit_size = 1;")
        }

        clwrite("if(id >= unit_size) {")
        clwrite("return;")
        clwrite("} else {")

        // Jump to the appropriate place to resume.
        clwrite("switch(control->ap_state_index) {")
        for (i <- 1 to currentState) {
            clwrite("case " + i + ": goto AP_STATE_" + i + ";")
        }
        clwrite("case -1: return;")
        clwrite("default: break;")
        clwrite("}")

        // Output the code.
        clwrite(nodeEmitter)

        clwrite("}")    // If ID is in range.
        clwrite("control->ap_state_index = 0;")
        clwrite("control->ap_ready = 1;")
        clwrite("}")    // While not blocked.
        clwrite("}")    // Function.
        write("};")

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

        // Write the source.
        val file = new File(parent, basename + ".cl")
        val ps = new PrintStream(new FileOutputStream(file))
        ps.print(emitSource)        
        ps.close

    }

}

