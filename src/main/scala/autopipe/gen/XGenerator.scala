package autopipe.gen

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import autopipe._

private[autopipe] object XGenerator extends Generator {

    private def getTypeName(vt: ValueType): String = vt match {
        case at: ArrayValueType     =>
            "array<" + at.baseType + ">[" + at.length + "]"
        case st: StructValueType    =>
            val fieldTypes = st.fields.map(f => getTypeName(f._2))
            "struct<" + fieldTypes.mkString(",") + ">"
        case td: TypeDefValueType  => td.value
        case _                            => vt.baseType.toString
    }

    private class KernelGenerator(val kt: KernelType) {

        def emitDecl: String = ""

        def emitKernels(kernels: Seq[Kernel]): String = ""

        def emitMapping: String = ""

    }

    private class ExternalKernelGenerator(_kt: KernelType)
        extends KernelGenerator(_kt) {

        private def emitInput(i: InputSymbol): String = {
            "    input " + getTypeName(i.valueType) + " " + i.name + ";\n"
        }

        private def emitOutput(o: OutputSymbol): String = {
            "    output " + getTypeName(o.valueType) + " " + o.name + ";\n"
        }

        private def emitConfig(c: ConfigSymbol): String = {
            "    config " + getTypeName(c.valueType) + " " + c.name +
            " = " + c.value + ";\n"
        }

        override def emitDecl = {
            val configs = kt.configs
            val inputs = kt.inputs
            val outputs = kt.outputs
            "block " + kt.name + " {\n" +
                configs.foldLeft("")((a, b) => a + emitConfig(b)) +
                inputs.foldLeft("")((a, b) => a + emitInput(b)) +
                outputs.foldLeft("")((a, b) => a + emitOutput(b)) +
            "};\n" +
            "platform " + kt.platform + " {\n" +
            "    impl " + kt.name +
            " (base=\"" + kt.name + "\");\n" +
            "};\n"
        }

        override def emitKernels(kernels: Seq[Kernel]): String = {
            val strs = kernels.map { k =>
                "    " + k.name + "\n"
            }
            strs.mkString
        }

    }

    private class InternalKernelGenerator(_kt: KernelType)
        extends ExternalKernelGenerator(_kt) {
    }

    private object ExDMA {

        var generated = false
        var label = ""

        def getLabel(e: Edge): String = {
            if (generated) {
                return label
            } else {
                generated = true
                label = e.label
                return label
            }
        }

    }

    private class EdgeGenerator(val edge: Edge) {

        def emitDecl(streams: Seq[Stream]): String = {

            val label = edge.label
            val count = streams.size

            def emitSHM(s: Stream) = {
                "    ({ " + s.sourceKernel.device.procName +
                ", " + s.destKernel.device.procName +
                "}, queuelength=8192)"
            }

            edge match {
                case c2c: CPU2CPU  =>
                    "resource " + label + "[" + count + "] is SHM {\n" +
                        streams.foldLeft("") { (a, s) =>
                            (if(!a.isEmpty) a + ",\n" else "") + emitSHM(s)
                        } +
                    "\n};\n"
                case _ => ""
            }

        }

        def emitMapping(streams: Seq[Stream]): String = {
            val label = edge.label
            edge match {
                case _ =>
                    streams.zipWithIndex.foldLeft("") { (a, s) =>
                        a + "map " + label + "[" + (s._2 + 1) + "] = { " +
                            "app." + s._1.label + " };\n";
                    }
            }
        }

    }

    private def createGenerator(kt: KernelType) = kt match {
        case in: InternalKernelType => new InternalKernelGenerator(in)
        case ex: ExternalKernelType => new ExternalKernelGenerator(ex)
        case _ => sys.error("Unknown block type")
    }

    private[autopipe] def emit(ap: AutoPipe) {

        var declStr = ""
        var blockStr = ""
        var mapStr = ""
        val kernelTypes = ap.kernels.map(_.kernelType).toSet
        for (kt <- kernelTypes) {
            val kernels = ap.kernels.filter(_.kernelType == kt)
            val generator = createGenerator(kt)
            declStr += generator.emitDecl
            blockStr += generator.emitKernels(kernels)
            mapStr += generator.emitMapping
        }

        val edgeTypes = new HashMap[Edge, ListBuffer[Stream]]
        for (s <- ap.streams) {
            if (s.edge != null) {
                edgeTypes.get(s.edge) match {
                    case Some(set) => set += s
                    case None =>
                        val set = new ListBuffer[Stream]
                        set += s
                        edgeTypes += ((s.edge, set))
                }
            }
        }
        for (e <- edgeTypes) {
            val generator = new EdgeGenerator(e._1)
            declStr += generator.emitDecl(e._2)
            mapStr += generator.emitMapping(e._2)
        }

        for (d <- ap.devices) {
            mapStr += d.emit
        }
        val edgeStr = ap.streams.foldLeft("") { (a, s) =>
            val sourcePort = s.sourceKernel.outputName(s.sourcePort)
            val destPort = s.destKernel.inputName(s.destPort)
            val sourceString = s.sourceKernel.label + "." + sourcePort
            val destString = s.destKernel.label + "." + destPort
            a + s.label + ": " + sourceString + " -> " + destString + ";\n"
        }
        var measureStr = ""
        for (s <- ap.streams) {
            for (m <- s.measures) {
                measureStr += m.emit(s)
            }
        }

        // Create the output string.
        write("#include \"std.x\"")
        write("#include \"ipc.x\"")
        write(declStr)
        write("block top {\n")
        write(blockStr)
        write
        write(edgeStr)
        write("};")
        write("use top app;")
        write(mapStr)
        write(measureStr)

    }

}

