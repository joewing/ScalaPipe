package autopipe.gen

import autopipe._

private[gen] trait HDLGenerator extends Generator {

    protected val kt: KernelType

    protected val ramWidth = 32

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

    def emitRAM(depth: Int) {
        val lastIndex = depth - 1
        val top = ramWidth - 1
        val wordBytesTop = ramWidth / 8 - 1

        // Inputs
        write(s"reg [$wordBytesTop:0] ram_mask;")
        write(s"reg [31:0] ram_addr;")
        write(s"reg [31:0] ram_state;")
        write(s"wire [$top:0] ram_in;")
        write(s"wire ram_we;")

        // Outputs
        write(s"wire ram_ready = 1;")
        write(s"reg [$top:0] ram_out;")

        write(s"reg [$top:0] ram_data [0:$lastIndex];")
        write(s"wire [31:0] ram_index = ram_addr + ram_state;")
        write(s"always @(posedge clk) begin")
        enter
        write(s"ram_out <= ram_data[ram_index];")
        write(s"if (ram_we) begin")
        enter
        write(s"ram_data[ram_index] <= ram_in;")
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
        }
    }

}
