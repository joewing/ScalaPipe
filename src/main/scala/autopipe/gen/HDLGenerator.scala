package autopipe.gen

import autopipe._

private[gen] trait HDLGenerator extends Generator {

    protected val kt: KernelType

    private val ramWidth = 32

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
        val block = graph.block(label)
        if (block.continuous) {
            getNextState(graph, graph.links(block).head.label)
        } else {
            block.label
        }
    }

    def getTypeString(name: String, vt: ValueType): String = {
        val upper = vt.bits - 1
        if (vt.signed) {
            s"signed [$upper:0] $name"
        } else {
            s"[$upper:0] $name"
        }
    }

    def emitRAM(name: String, vt: ValueType) {
        val top = ramWidth - 1
        val size = (vt.bits + ramWidth - 1) / ramWidth
        write(s"reg [$top:0] $name [0:$size];")
        write(s"wire [31:0] ${name}_wix;")
        write(s"wire [31:0] ${name}_rix;")
        write(s"wire [$top:0] ${name}_in;")
        write(s"reg [$top:0] ${name}_out;")
        write(s"wire ${name}_we;")
        write(s"always @(posedge clk) begin")
        enter
        write(s"${name}_out <= $name[${name}_rix];")
        write(s"if (${name}_we) begin")
        enter
        write(s"$name[${name}_wix] <= ${name}_in;")
        leave
        write("end")
        leave
        write("end")
        write
    }

    def emitLocal(name: String, s: BaseSymbol) {
        if (s.valueType.flat) {
            val ts = getTypeString(name, s.valueType)
            if (s.isRegister) {
                write(s"reg $ts;")
            } else {
                write(s"wire $ts;")
            }
        } else {
            emitRAM(name, s.valueType)
        }
    }

}
