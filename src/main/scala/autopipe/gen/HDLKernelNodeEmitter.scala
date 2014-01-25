package autopipe.gen

import autopipe._
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

    override def emitBegin(block: StateBlock) {
        write("if (state == " + block.label + ") begin")
        enter
        beginScope
        block.nodes.foreach { node =>
            val localInputs = getLocalInputs(node)
            val localOutputs = getLocalOutputs(node)
            val ports = localInputs ++ localOutputs
            val portsToCheck = ports.filter { !isCheckedPort(_) }
            addCheckedPorts(portsToCheck)
        }
        write("if (guard_" + block.label + ") begin")
        enter
    }

    override def emitEnd(block: StateBlock) {
        val portsToRelease = getCheckedPorts
        for (i <- portsToRelease if kt.isInput(i)) {
            moduleEmitter.addReadState(block.label, i)
        }
        leave
        write("end")
        endScope
        leave
        write("end")
    }

    override def emitAvailable(block: StateBlock, node: IRInstruction) {
        val dest = emitSymbol(node.dest)
        val src = node.srca match {
            case is: InputSymbol     => "avail_" + is.name
            case os: OutputSymbol    => "!afull_" + os.name
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
                    write(dest + " <= " + src + ";")
                }
        }
    }

    override def emitStop(block: StateBlock, node: IRStop) {
        write("state <= 0;")
    }

    override def emitReturn(block: StateBlock, node: IRReturn) {
        val src = emitSymbol(node.result)
        val dest = kt.outputs.head.name
        moduleEmitter.addWriteState(block.label, dest, src)

        val startState = graph.nodes.collect { case s: IRStart => s }.head.next
        write("state <= " + startState + ";")

    }

    override def start {
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

}
