package autopipe.gen

import autopipe._

private[autopipe] class OpenCLKernelNodeEmitter(
        _kt: InternalKernelType,
        val gen: StateTrait,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(_kt, _timing) with ASTUtils {

    private def setState(state: Int): String =
        "control->ap_state_index = " + state + ";"

    private def canWrite(name: String, index: Int): String =
        "(control->" + name + "_sent < control->" + name + "_size)"

    private def canRead(name: String, index: Int): String =
        "(control->" + name + "_read < control->" + name + "_size)"

    private def allocate(t: ValueType, name: String, index: Int): String = ""

    private val allocateBlocks = false

    private def inputAvailable(name: String): String = {
        "(control->" + name + "_size > control->" + name + "_read)"
    }

    private def outputFull(name: String): String = {
        "control->" + name + "_size == control->" + name + "_sent"
    }

    private def send(name: String, index: Int): String = {
        "barrier(CLK_GLOBAL_MEM_FENCE); " +
        "if(id == 0) { " + 
        "control->" + name + "_sent += unit_size; " +
        "} " +
        "barrier(CLK_GLOBAL_MEM_FENCE);"
    }

    private def release(name: String, index: Int, count: Int): String = {
        if (count > 0) {
            "barrier(CLK_GLOBAL_MEM_FENCE); " +
            "if(id == 0) { " +
            "control->" + name + "_read += " + count + " * unit_size; " +
            "} " +
            "barrier(CLK_GLOBAL_MEM_FENCE); "
        } else {
            ""
        }
    }

    private def ret(result: Int): String = {
        "control->ap_ready = 1; " +
        "return;"
    }

    private def derefInput(name: String): String =
        name + "[control->" + name + "_read + id]"

    private def derefOutput(name: String): String =
        name + "[control->" + name + "_sent + id]"

    override def emitAvailable(node: ASTAvailableNode): String = {
        val name = node.symbol
        if (kt.isInput(name)) {
            val index = kt.inputIndex(name)
            canRead(name, index)
        } else if (kt.isOutput(name)) {
            val index = kt.outputIndex(name)
            canWrite(name, index)
        } else {
            Error.raise("argument to avail must be an input or output", node)
        }
    }

    private def emitComponent(base: String,
                              vt: ValueType,
                              comp: ASTNode): (String, ValueType) = {
        val lit: SymbolLiteral = comp match {
            case sl: SymbolLiteral => sl
            case _ => null
        }
        val nvt = vt match {
            case at: ArrayValueType                 => at.itemType
            case st: StructValueType if lit != null => st.fields(lit.symbol)
            case ut: UnionValueType  if lit != null => ut.fields(lit.symbol)
            case nt: NativeValueType if lit != null => ValueType.any
            case _                                  => sys.error("internal")
        }
        val expr = if (lit != null) {
            "." + comp
        } else {
            "[" + emitExpr(comp) + "]"
        }
        val str = if (isNative(vt)) {
            s"$base$expr"
        } else if (isNativePointer(vt)) {
            s"$base(*$vt)$expr"
        } else if (comp.isInstanceOf[SymbolLiteral]) {
            s"$base$expr"
        } else {
            s"$base.values$expr"
        }
        return ((str, nvt))
    }

    override def emitSymbol(node: ASTSymbolNode): String = {
        val name = node.symbol
        val base = if (kt.isPort(name)) {
            s"(*$name)"
        } else if (kt.isLocal(name)) { 
            s"$name"
        } else if (kt.isState(name) || kt.isConfig(name)) {
            s"block->$name"
        } else {
            Error.raise(s"symbol not declared: $name", node)
        }
        val valueType = kt.getType(node)
        val start = ((base, valueType))
        val result = node.indexes.foldLeft(start) { (a, index) =>
            val (base, vt) = a
            emitComponent(base, vt, index)
        }
        return result._1
    }

    override def emitAssign(node: ASTAssignNode) {

        var outputs = localOutputs(node)
        for (o <- outputs) {
            val oindex = kt.outputIndex(o)
            val valueType = kt.outputs(oindex).valueType
            if (!allocateBlocks) {
                writeLeft("AP_STATE_" + gen.nextState + ":")
                write("if(" + outputFull(o) + ") {")
                enter
                write(setState(gen.currentState))
                write(ret(0))
                leave
                write("}")
            }
            write(allocate(valueType, o, oindex))
        }

        write(emitExpr(node.dest) + " = " + emitExpr(node.src) + ";")

        updateClocks(getTiming(node))

        for (o <- outputs) {
            val oindex = kt.outputIndex(o)
            write(send(o, oindex))
        }

    }

    override def emitStop(node: ASTStopNode) {
        updateClocks(getTiming(node))
        write(setState(-1))
        write(ret(1))
    }

    override def emitReturn(node: ASTReturnNode) {
        val output = kt.outputs(0).name
        val valueType = kt.outputs(0).valueType
        if (!allocateBlocks) {
            writeLeft("AP_STATE_" + gen.nextState + ":")
            write("if(" + outputFull(output) + ") {")
            enter
            write(setState(gen.currentState))
            write(ret(0))
            leave
            write("}")
        }
        write(allocate(valueType, output, 0))
        write("*output = " + emitExpr(node.a) + ";")
        updateClocks(getTiming(node))
        write(send(output, 0))
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write("block->ap_clocks += " + count + ";")
        }
    }

    override def checkInputs(node: ASTNode): Int = {
        beginScope
        val portsToCheck = blockingInputs(node).filter { !isCheckedPort(_) }
        if (!portsToCheck.isEmpty)  {
            addCheckedPorts(portsToCheck)
            writeLeft("AP_STATE_" + gen.nextState + ":")
            write("if(" +
                  portsToCheck.map(inputAvailable(_)).mkString(" && ") +
                  ") {")
            enter
        }
        gen.currentState
    }

    override def releaseInputs(node: ASTNode, state: Int) {
        val portsToRelease = getCheckedPorts
        endScope
        val inputs = kt.inputs.map({ _.name }).filter({ !isCheckedPort(_) })
        if (!portsToRelease.isEmpty) {
            for (i <- inputs) {
                val index = kt.inputIndex(i)
                if (portsToRelease.contains(i)) {
                    write(release(i, index, 1))
                } else {
                    // FIXME: this is wasteful and may not be necessary.
                    // We need to ensure that we get called back if we return
                    // without consuming data for another port.
                    write("if(" + i + " != 0) {")
                    enter
                    write(release(i, index, 0))
                    leave
                    write("}")
                }
            }
            write(setState(0))
            leave
            write("} else {")
            enter
            write(setState(state))
            write(ret(0))
            leave
            write("}")
        }
    }

}
