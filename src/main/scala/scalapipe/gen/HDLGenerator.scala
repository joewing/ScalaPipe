package scalapipe.gen

import scalapipe._

private[gen] trait HDLGenerator extends Generator {

    protected val kt: KernelType

    protected val ramWidth = kt.sp.parameters.get[Int]('memoryWidth)
    protected val ramAddrWidth = kt.sp.parameters.get[Int]('memoryAddrWidth)

    def hasState(node: ASTNode): Boolean = node match {
        case in: ASTIfNode      => in.iFalse != null
        case bn: ASTBlockNode   => bn.children.size > 1
        case _                  => false
    }

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
        case is: InputSymbol        => "input_" + is.name
        case ts: TempSymbol         => "temp" + ts.id
        case ss: StateSymbol        => "state_" + ss.name
        case cs: ConfigSymbol       => cs.name
        case im: ImmediateSymbol    => getImmediate(im)
        case null                   => ""
        case _ => sys.error(s"internal: $sym")
    }

    def getNextState(graph: IRGraph, label: Int): Int = {

        var visited = Set[Int]()

        def next(l: Int): Int = {
            if (visited.contains(l)) {
                return 0
            } else {
                visited += l
                val block = graph.block(l)
                if (block.continuous) {
                    return next(graph.links(block).head.label)
                } else {
                    return l
                }
            }
        }

        return next(label)
    }

    def getTypeString(name: String, vt: ValueType): String = {
        val upper = vt.bits - 1
        if (vt.signed) {
            s"signed [$upper:0] $name"
        } else {
            s"[$upper:0] $name"
        }
    }

    private def emitLocal(name: String, s: BaseSymbol) {
        if (s.valueType.flat) {
            val ts = getTypeString(name, s.valueType)
            if (s.isRegister) {
                write(s"reg $ts;")
            } else {
                write(s"wire $ts;")
            }
        }
    }

    def emitLocals {
        for (s <- kt.states) {
            emitLocal("state_" + s.name, s)
        }
        for (t <- kt.temps) {
            emitLocal("temp" + t.id, t)
        }
        if (kt.ramDepth > 0) {
            write(s"reg [31:0] ram_state;")
        }
        write("reg [31:0] state;")
        write("reg [31:0] last_state;")
    }

}
