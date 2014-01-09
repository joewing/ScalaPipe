
package autopipe.gen

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import autopipe._

private[autopipe] object XGenerator extends Generator {

    private def getTypeName(vt: ValueType): String = vt match {
        case at: ArrayValueType     =>
            "array<" + at.baseType + ">[" + at.length + "]"
        case st: StructValueType    =>
            sys.error("structs not yet supported")
        case td: TypeDefValueType  => td.value
        case _                            => vt.baseType.toString
    }

    private class BlockGenerator(val bt: BlockType) {

        def emitDecl: String = ""

        def emitBlocks(blocks: Seq[Block]): String = ""

        def emitMapping: String = ""

    }

    private class ExternalBlockGenerator(_bt: BlockType)
        extends BlockGenerator(_bt) {

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
            val configs = bt.configs
            val inputs = bt.inputs
            val outputs = bt.outputs
            "block " + bt.name + " {\n" +
                configs.foldLeft("")((a, b) => a + emitConfig(b)) +
                inputs.foldLeft("")((a, b) => a + emitInput(b)) +
                outputs.foldLeft("")((a, b) => a + emitOutput(b)) +
            "};\n" +
            "platform " + bt.platform + " {\n" +
            "    impl " + bt.name +
            " (base=\"" + bt.name + "\");\n" +
            "};\n"
        }

        override def emitBlocks(blocks: Seq[Block]): String = {
            blocks.foldLeft("")((a, b) => a + b.emit)
        }

    }

    private class InternalBlockGenerator(_bt: BlockType)
        extends ExternalBlockGenerator(_bt) {
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
                "    ({ " + s.sourceBlock.device.procName +
                ", " + s.destBlock.device.procName +
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

    private def createGenerator(bt: BlockType) = bt match {
        case in: InternalBlockType => new InternalBlockGenerator(in)
        case ex: ExternalBlockType => new ExternalBlockGenerator(ex)
        case _ => sys.error("Unknown block type")
    }

    private[autopipe] def emit(ap: AutoPipe) {

        var declStr = ""
        var blockStr = ""
        var mapStr = ""
        for (bt <- ap.getBlockTypes) {
            if (!bt.blocks.isEmpty) {
                val generator = createGenerator(bt)
                declStr += generator.emitDecl
                blockStr += generator.emitBlocks(bt.blocks)
                mapStr += generator.emitMapping
            }
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
            val sourcePort = s.sourceBlock.outputName(s.sourcePort)
            val destPort = s.destBlock.inputName(s.destPort)
            val sourceString = s.sourceBlock.label + "." + sourcePort
            val destString = s.destBlock.label + "." + destPort
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

