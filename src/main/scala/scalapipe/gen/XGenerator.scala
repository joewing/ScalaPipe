package scalapipe.gen

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import scalapipe._

private[scalapipe] object XGenerator extends Generator {

    private def getTypeName(vt: ValueType): String = vt match {
        case at: ArrayValueType =>
            s"array<${at.baseType}>[${at.length}]"
        case st: StructValueType =>
            val fieldTypes = st.fieldTypes.map(getTypeName(_))
            val fieldString = fieldTypes.mkString(",")
            s"struct<$fieldString>"
        case td: TypeDefValueType => td.value
        case _ => vt.baseType.toString
    }

    private class KernelGenerator(val kt: KernelType) {

        private def emitInput(i: InputSymbol): String = {
            val typeName = getTypeName(i.valueType)
            s"    input $typeName ${i.name};\n"
        }

        private def emitOutput(o: OutputSymbol): String = {
            val typeName = getTypeName(o.valueType)
            s"    output $typeName ${o.name};\n"
        }

        private def emitConfig(c: ConfigSymbol): String = {
            val typeName = getTypeName(c.valueType)
            s"    config $typeName ${c.name} = ${c.value}\n";
        }

        def emitDecl = {
            val configs = kt.configs.foldLeft("") { (a, b) =>
                a + emitConfig(b)
            }
            val inputs = kt.inputs.foldLeft("") { (a, b) =>
                a + emitInput(b)
            }
            val outputs = kt.outputs.foldLeft("") { (a, b) =>
                a + emitOutput(b)
            }
            s"block ${kt.name} {\n$configs$inputs$outputs};\n" +
            s"platform ${kt.platform} {\n" +
            s"""    impl ${kt.name}(base=\"${kt.name}\");\n};\n"""
        }

        def emitKernels(kernels: Seq[KernelInstance]): String = {
            val strs = kernels.map { k =>
                if (k.configs.isEmpty) {
                    s"    ${k.name} ${k.label};\n"
                } else {
                    val configs = k.configs.map { case (name, value) =>
                        s"${name}=${value}"
                    }.mkString(",")
                    s"    ${k.name} ${k.label}($configs);\n"
                }
            }
            strs.mkString
        }

        def emitMapping: String = {
            // TODO
            ""
        }

    }

    private class EdgeGenerator(val edge: Edge) {

        def emitDecl(streams: Seq[Stream]): String = {

            val label = edge.label
            val count = streams.size

            def emitSHM(s: Stream) = {
                "    ({ TODO, TODO }, queuelength=8192)"
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

    private[scalapipe] def emit(sp: ScalaPipe) {

        var declStr = ""
        var blockStr = ""
        var mapStr = ""
        val kernelTypes = sp.instances.map(_.kernelType).toSet
        for (kt <- kernelTypes) {
            val kernels = sp.instances.filter(_.kernelType == kt)
            val generator = new KernelGenerator(kt)
            declStr += generator.emitDecl
            blockStr += generator.emitKernels(kernels)
            mapStr += generator.emitMapping
        }

        val edgeTypes = new HashMap[Edge, ListBuffer[Stream]]
        for (s <- sp.streams) {
            if (s.edge != null) {
                edgeTypes.get(s.edge) match {
                    case Some(set) => set += s
                    case None =>
                        val set = new ListBuffer[Stream]
                        set += s
                        edgeTypes += (s.edge -> set)
                }
            }
        }
        for (e <- edgeTypes) {
            val generator = new EdgeGenerator(e._1)
            declStr += generator.emitDecl(e._2)
            mapStr += generator.emitMapping(e._2)
        }

        val edgeStr = sp.streams.foldLeft("") { (a, s) =>
            val sourcePort = s.sourceKernel.outputName(s.sourcePort)
            val destPort = s.destKernel.inputName(s.destPort)
            val sourceString = s"${s.sourceKernel.label}.${sourcePort}"
            val destString = s"${s.destKernel.label}.${destPort}"
            s"$a${s.label}: $sourceString -> $destString;\n"
        }
        val measureStr = sp.streams.foldLeft("") { (a, s) =>
            s.measures.foldLeft(a) { (b, m) => b + m.emit(s) }
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

