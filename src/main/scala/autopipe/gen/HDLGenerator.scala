
package autopipe.gen

import autopipe._

private[gen] trait HDLGenerator extends Generator {

    def hasState(node: ASTNode): Boolean = node match {
        case in: ASTIfNode        => in.iFalse != null
        case bn: ASTBlockNode    => bn.children.size > 1
        case _                        => false
    }

    def useFlatMemory(vt: ValueType): Boolean = vt.bits <= 1024

    def getImmediate(s: ImmediateSymbol): String = s.value match {
        case f: FloatLiteral =>
            f.valueType match {
                case ValueType.float32 => f.rawFloat.toString
                case ValueType.float64 => f.rawDouble.toString
                case _ =>
                    Error.raise("unsupported float type: " + f.valueType)
            }
        case _ => s.value.toString
    }

    def emitSymbol(sym: BaseSymbol): String = sym match {
        case is: InputSymbol         => "input_" + is.name
        case ts: TempSymbol          => "temp" + ts.id
        case ss: StateSymbol         => "state_" + ss.name
        case cs: ConfigSymbol        => cs.name
        case im: ImmediateSymbol    => getImmediate(im)
        case null                        => ""
        case _ => sys.error("internal: " + sym)
    }

    def getNextState(graph: IRGraph, label: Int): Int = {
        val block = graph.block(label)
        if (block.continuous) {
            getNextState(graph, graph.links(block).head.label)
        } else {
            block.label
        }
    }

    def getPortTypeString(name: String, vt: ValueType): String = {
        val upper = vt.bits - 1
        if (vt.signed) {
            "signed [" + upper + ":0] " + name
        } else {
            "[" + upper + ":0] " + name
        }
    }

    def getTypeString(name: String, vt: ValueType): String = vt match {
        case at: ArrayValueType =>
            getPortTypeString(name, at.itemType) + "[0:" + (at.length - 1) + "]"
        case _ => getPortTypeString(name, vt)
    }

    def emitFlatMemory(name: String, vt: ArrayValueType) {
        val ats = getPortTypeString(name, vt)
        write("reg " + ats + ";")
    }

    def emitLocalArray(name: String, vt: ArrayValueType) {
        val ats = getTypeString(name, vt)
        val inStr = getPortTypeString(name + "_in", vt.itemType)
        val outStr = getPortTypeString(name + "_out", vt.itemType)
        write("reg " + ats + ";")
        write("wire [31:0] " + name + "_wix;")
        write("wire [31:0] " + name + "_rix;")
        write("wire " + inStr + ";")
        write("reg " + outStr + ";")
        write("wire " + name + "_we;")
        write
        write("always @(posedge clk) begin")
        enter
        write(name + "_out <= " + name + "[" + name + "_rix];")
        write("if (" + name + "_we) begin")
        enter
        write(name + "[" + name + "_wix] <= " + name + "_in;")
        leave
        write("end")
        leave
        write("end")
        write
    }

    def emitLocal(name: String, s: BaseSymbol) {
        s.valueType match {
            case avt: ArrayValueType =>
                if (useFlatMemory(avt)) {
                    emitFlatMemory(name, avt)
                } else {
                    emitLocalArray(name, avt)
                }
            case _ =>
                val ts = getTypeString(name, s.valueType)
                if (s.isRegister) {
                    write("reg " + ts + ";")
                } else {
                    write("wire " + ts + ";")
                }
        }
    }

}

