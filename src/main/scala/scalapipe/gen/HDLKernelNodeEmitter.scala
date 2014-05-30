package scalapipe.gen

import scalapipe._
import scala.collection.immutable.HashSet

private[gen] class HDLKernelNodeEmitter(
        _kt: InternalKernelType,
        _graph: IRGraph,
        _moduleEmitter: HDLModuleEmitter
    ) extends HDLNodeEmitter(_kt, _graph, _moduleEmitter) {

    private def getLocalPorts(node: IRNode, p: BaseSymbol => Boolean) = {
        node match {
            case in: IRInstruction if (in.op == NodeType.avail) =>
                    List(in.dest).filter(p)
            case _ => node.symbols.filter(p)
        }
    }

    private def getLocalInputs(node: IRNode) =
        getLocalPorts(node, { _.isInstanceOf[InputSymbol] } ).map { _.name }

    private def getLocalOutputs(node: IRNode) =
        getLocalPorts(node, { _.isInstanceOf[OutputSymbol] } ).map { _.name }

    override def checkPorts(block: StateBlock) {
        block.nodes.foreach { node =>
            val localInputs = getLocalInputs(node)
            val localOutputs = getLocalOutputs(node)
            val ports = localInputs ++ localOutputs
            val portsToCheck = ports.filter { !isCheckedPort(_) }
            addCheckedPorts(portsToCheck)
        }
    }

    override def releasePorts(block: StateBlock) {
        val portsToRelease = getCheckedPorts
        for (i <- portsToRelease if kt.isInput(i)) {
            moduleEmitter.addReadState(block.label, i)
        }
    }

    override def emitAvailable(block: StateBlock, node: IRInstruction) {
        val dest = emitSymbol(node.dest)
        val src = node.srca match {
            case is: InputSymbol     => "avail_" + is.name
            case os: OutputSymbol    => "!full_" + os.name
            case _ => sys.error("internal")
        }
        if (block.continuous) {
            moduleEmitter.addAssignment(dest + " <= " + src + ";")
        } else {
            write(dest + " <= " + src + ";")
        }
    }

    override def emitAssign(block: StateBlock, node: IRInstruction) {
        val src = emitSymbol(node.srca)
        node.dest match {
            case os: OutputSymbol =>
                moduleEmitter.addWriteState(block.label, os.name, src)
            case _ =>
                val dest = emitSymbol(node.dest)
                if (block.continuous) {
                    moduleEmitter.addAssignment(dest + " <= " + src + ";")
                } else {
                    write(s"$dest <= $src;")
                }
        }
    }

    override def emitStop(block: StateBlock, node: IRStop) {
        write("running <= 0;")
    }

    override def emitReturn(block: StateBlock, node: IRReturn) {
        val src = emitSymbol(node.result)
        val dest = kt.outputs.head.name
        moduleEmitter.addWriteState(block.label, dest, src)

        val startState = graph.nodes.collect { case s: IRStart => s }.head.next
        write("state <= " + startState + ";")

    }

    override def start {
        if (kt.ramDepth > 0) {
            write("ram_re <= 0;")
            write("ram_we <= 0;")
        }
        write("if (rst) begin")
        enter
        val blocking = graph.blocks.filter(!_.continuous)
        val dests = blocking.flatMap(_.dests)
        val pdests = HashSet(dests: _*).filter { d => d.valueType match {
                case ft: FloatValueType => true
                case ft: FixedValueType => true
                case it: IntegerValueType => true
                case _ => false
            }
        }
        val toset = pdests.filter { d => d match {
                case ss: StateSymbol => ss.value == null
                case ts: TempSymbol => true
                case _ => false
            }
        }
        toset.foreach { s =>
            write(emitSymbol(s) + " <= 0;")
        }
    }

    override def stop {
        leave
        write("end")
    }

    override def checkRunning(block: StateBlock) {
        val inputs = block.srcs.collect { case i: InputSymbol => i }
        if (!inputs.isEmpty) {
            val inputSymbols = inputs.map { i => "avail_" + i.name }
            val waitingString = inputSymbols.mkString(" | ")
            write(s"running <= $waitingString;")
        }
    }

}
