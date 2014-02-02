
package scalapipe.gen

import scala.collection.mutable.HashSet
import java.io.File

import scalapipe._

private[scalapipe] class OpenCLKernelGenerator(
        _kt: InternalKernelType
    ) extends KernelGenerator(_kt) with StateTrait {

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
            case ValueType.unsigned16   => "ushort"
            case ValueType.signed16     => "short"
            case ValueType.unsigned32   => "uint"
            case ValueType.signed32     => "int"
            case ValueType.unsigned64   => "ulong"
            case ValueType.signed64     => "long"
            case ValueType.float32      => "float"
            case ValueType.float64      => "double"
            case a: ArrayValueType      =>
                getOpenCLType(a.itemType) + s"[${a.length}]"
            case _ =>
                Error.raise(s"Type $t not supported in OpenCL kernels")
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

    protected def emitFunction {
    }

    private def emitSource: String = {

        write("static const char *" + kt.name + "_source[] = {")

        clwrite("#line 3 \\\"" + kt.name + ".cl\\\"")
        clwrite("#pragma OPENCL EXTENSION cl_khr_byte_addressable_store: " +
                "enable")
        clwrite("#pragma OPENCL EXTENSION cl_khr_fp64: enable")

        // Write typedefs.
        val emittedTypes = new HashSet[ValueType]
        kt.configs.foreach { c => emitType(c.valueType, emittedTypes) }
        kt.states.foreach { s => emitType(s.valueType, emittedTypes) }
        kt.inputs.foreach { i => emitType(i.valueType, emittedTypes) }
        kt.outputs.foreach { o => emitType(o.valueType, emittedTypes) }
        emitType(ValueType.signed8, emittedTypes)    // Booleans
        emitType(ValueType.signed32, emittedTypes)  // Integer literals
        emitType(ValueType.float64, emittedTypes)    // Float literals

        // Write the block control struct.
        clwrite("typedef struct {")
        clwrite("int ap_ready;")
        clwrite("int ap_state_index;")
        for (i <- kt.inputs) {
            clwrite("int " + i.name + "_size;")
            clwrite("int " + i.name + "_read;")
        }
        for (o <- kt.outputs) {
            clwrite("int " + o.name + "_size;")
            clwrite("int " + o.name + "_sent;")
        }
        clwrite("} " + kt.name + "_control;")

        // Write the block data struct.
        clwrite("typedef struct {")
        for (s <- kt.states if !s.isLocal) {
            clwrite(s.valueType.name + " " + s.name + ";")
        }
        clwrite("} " + kt.name + "_data;")

        // Start the kernel function.
        clwrite("__kernel void " + kt.name +
                  "(__global " + kt.name + "_control *control, " +
                  "__global " + kt.name + "_data *block")

        // Input ports.
        for (i <- kt.inputs) {
            clwrite(", __global " + i.valueType + " *" + i.name)
        }

        // Output ports.
        for (o <- kt.outputs) {
            clwrite(", __global " + o.valueType + " *" + o.name)
        }

        clwrite(") {")

        // Declare locals.
        for (l <- kt.states if l.isLocal) {
            clwrite(l.valueType.name + " " + l.name + ";")
        }

        // Generate the kernel code.
        val nodeEmitter = new OpenCLKernelNodeEmitter(kt, this, null)
        nodeEmitter.emit(kt.expression)

        clwrite("for(;;) {")

        clwrite("const int id = get_local_id(0);")
        if (kt.states.filter { s => !s.isLocal }.isEmpty) {
            clwrite("int unit_size = get_local_size(0);")
            for (i <- kt.inputs) {
                clwrite("unit_size = min(unit_size, " +
                          "control->" + i.name + "_size - " +
                          "control->" + i.name + "_read);")
            }
            for (i <- kt.outputs) {
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

        emitFunction

        getOutput

    }

    override def emit(dir: File) {

        import java.io.{FileOutputStream, PrintStream}

        // Create the block directory.
        val parent = new File(dir, kt.name)
        parent.mkdir

        // Write the source.
        val file = new File(parent, kt.name + ".cl")
        val ps = new PrintStream(new FileOutputStream(file))
        ps.print(emitSource)
        ps.close

    }

}

