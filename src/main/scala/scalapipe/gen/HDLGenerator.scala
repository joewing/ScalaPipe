package scalapipe.gen

import scalapipe._

private[gen] trait HDLGenerator extends Generator {

    protected val kt: KernelType

    protected val ramWidth = kt.sp.parameters.get[Int]('memoryWidth)

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

    private def emitRAM(depth: Int) {
        val lastIndex = depth - 1
        val top = ramWidth - 1
        val wordBytes = ramWidth / 8
        val wordBytesTop = wordBytes - 1

        // Inputs
        write(s"reg [$wordBytesTop:0] ram_mask;")
        write(s"reg [31:0] ram_addr;")
        write(s"reg [31:0] ram_state;")
        write(s"reg [$top:0] ram_in;")
        write(s"reg ram_we;")
        write(s"reg ram_re;")

        // Outputs
        write(s"reg ram_ready;")
        write(s"reg [$top:0] ram_out;")

        write(s"reg [$top:0] ram_data [0:$lastIndex];")
        write(s"always @(posedge clk) begin")
        enter
        write(s"ram_ready <= !(ram_re | ram_we);")
        write(s"if (ram_re) begin")
        enter
        write(s"ram_out <= ram_data[ram_addr];")
        leave
        write(s"end")
        write(s"if (ram_we) begin")
        enter
        for (i <- 0 until wordBytes) {
            val bottom = 8 * i
            val top = bottom + 7
            write(s"if (ram_mask[$i]) begin")
            enter
            write(s"ram_data[ram_addr][$top:$bottom] <= ram_in[$top:$bottom];")
            leave
            write(s"end")
        }
        leave
        write("end")
        leave
        write("end")
        write
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

    def ramDepth(vt: ValueType): Int = {
        if (vt.flat) {
            return 0
        } else {
            return (vt.bits + ramWidth - 1) / ramWidth
        }
    }

    val ramDepth: Int = {
        val values = kt.states ++ kt.temps
        values.map(v => ramDepth(v.valueType)).sum
    }

    def emitLocals {
        for (s <- kt.states) {
            emitLocal("state_" + s.name, s)
        }
        for (t <- kt.temps) {
            emitLocal("temp" + t.id, t)
        }
        if (ramDepth > 0) {
            emitRAM(ramDepth)
        }
        write("reg [31:0] state;")
        write("reg [31:0] last_state;")
    }

}
