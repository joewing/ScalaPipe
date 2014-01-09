
package autopipe.gen

import autopipe._
import scala.collection.immutable.HashSet

private[gen] class HDLFunctionNodeEmitter(
        val ft: FunctionType,
        _graph: IRGraph,
        _moduleEmitter: HDLModuleEmitter
    ) extends HDLNodeEmitter(ft, _graph, _moduleEmitter) {

    override def emitBegin(block: StateBlock) {
        write("if (state == " + block.label + ") begin")
        enter
        beginScope
        write("if (guard_" + block.label + ") begin")
        enter
    }

    override def emitEnd(block: StateBlock) {
        leave
        write("end")
        endScope
        leave
        write("end")
    }

    override def emitAvailable(block: StateBlock, node: IRInstruction) {
        Error.raise("AVAIL not valid in a function")
    }

    override def emitAssign(block: StateBlock, node: IRInstruction) {
        val src = emitSymbol(node.srca)
        val dest = emitSymbol(node.dest)
        if (block.continuous) {
            moduleEmitter.addAssignment(dest + " <= " + src + ";")
        } else {
            write(dest + " <= " + src + ";")
        }
    }

    override def emitStop(block: StateBlock, node: IRStop) {
        Error.raise("stop not valid in a function")
    }

    override def emitReturn(block: StateBlock, node: IRReturn) {
        val src = emitSymbol(node.result)
        write("result_out <= " + src + ";")
        write("state <= 0;")
    }

    override def start {
        write("if (start) begin")
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

