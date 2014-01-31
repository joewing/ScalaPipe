package scalapipe.gen

import scalapipe._

private[scalapipe] class CKernelNodeEmitter(
        _kt: InternalKernelType,
        val gen: StateTrait,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(_kt, _timing) with ASTUtils {

    private def setState(state: Int) = s"block->ap_state_index = $state;"

    private def release(name: String, index: Int, count: Int) =
        s"ap_release(block, $index, $count); $name = NULL;"

    private def ret(result: Int) = s"return $result;"

    override def emitAvailable(node: ASTAvailableNode): String = {
        val name = node.symbol
        if (kt.isInput(name)) {
            s"($name != 0)"
        } else if (kt.isOutput(name)) {
            val index = kt.outputIndex(name)
            s"ap_get_free(block, $index)"
        } else {
            Error.raise("argument to avail must be an input or output", node)
        }
    }

    private def emitComponent(base: String,
                              vt: ValueType,
                              comp: ASTNode): (String, ValueType) = {
        val nvt = vt match {
            case at: ArrayValueType     => at.itemType
            case rt: RecordValueType    => rt.fieldType(comp)
            case nt: NativeValueType    => ValueType.any
            case _                      => ValueType.void
        }
        val expr = comp match {
            case sl: SymbolLiteral  => "." + comp
            case _                  => "[" + emitExpr(comp) + "]"
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
        result._1
    }

    override def emitAssign(node: ASTAssignNode) {

        var outputs = localOutputs(node)
        for (o <- outputs) {
            val oindex = kt.outputIndex(o)
            val vtype = kt.outputs(oindex).valueType.name
            write(s"$o = ($vtype*)ap_allocate(block, $oindex, 1);")
        }

        val dest = emitExpr(node.dest)
        val src = emitExpr(node.src)
        write(s"$dest = $src;")
        updateClocks(getTiming(node))

        for (oindex <- outputs.map(kt.outputIndex(_))) {
            write(s"ap_send(block, $oindex, 1);")
        }

    }

    override def emitStop(node: ASTStopNode) {
        updateClocks(getTiming(node))
        write(setState(-1))
        write(ret(1))
    }

    override def emitReturn(node: ASTReturnNode) {
        val name = kt.outputs(0).name
        val vtype = kt.outputs(0).valueType.name
        val src = emitExpr(node.a)
        write(s"$name = ($vtype*)ap_allocate(block, 0, 1);")
        write(s"*$name = $src;")
        updateClocks(getTiming(node))
        write(s"ap_send(block, 0, 1);")
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write(s"block->ap_clocks += $count;")
        }
    }

    override def checkInputs(node: ASTNode): Int = {

        def emitAccess(read: Boolean, name: String, index: ASTNode) {
            kt.getSymbol(name) match {
                case is: InputSymbol =>
                    assert(read)
                    write("fprintf(block->trace_fd, \"C" + is.index + "\\n\");")
                case os: OutputSymbol =>
                    assert(!read)
                    write("fprintf(block->trace_fd, \"P" + os.index + "\\n\");")
                case s: BaseSymbol =>
                    s.valueType match {
                        case at: ArrayValueType if at.bits >= 1024 =>
                            val offset = kt.getBaseOffset(name)
                            val bytes = (at.itemType.bits + 7) / 8
                            val ch = if (read) 'R' else 'W'
                            write("fprintf(block->trace_fd, \"" + ch +
                                  "%x:%x\\n\", " +
                                  offset + " + (" + emitExpr(index) +
                                  ") * " + bytes + ", " + bytes + ");")
                        case _ => ()
                    }
            }
        }

        beginScope
        val portsToCheck = blockingInputs(node).filter(!isCheckedPort(_))
        if (!portsToCheck.isEmpty)  {
            addCheckedPorts(portsToCheck)
            writeLeft("AP_STATE_" + gen.nextState + ":")
            val cond = portsToCheck.mkString(" && ")
            writeIf(cond)
        }
        if (kt.parameters.get('trace)) {
            for ((r, index) <- reads(node)) {
                emitAccess(true, r, index)
            }
            for ((w, index) <- writes(node)) {
                emitAccess(false, w, index)
            }
        }
        gen.currentState
    }

    override def releaseInputs(node: ASTNode, state: Int) {
        val portsToRelease = getCheckedPorts
        endScope
        val inputs = kt.inputs.map(_.name).filter(!isCheckedPort(_))
        if (!portsToRelease.isEmpty) {
            for (i <- inputs) {
                val index = kt.inputIndex(i)
                if (portsToRelease.contains(i)) {
                    write(release(i, index, 1))
                } else {
                    // FIXME: this is wasteful and may not be necessary.
                    // We need to ensure that we get called back if we return
                    // without consuming data for another port.
                    writeIf(s"$i != 0")
                    write(release(i, index, 0))
                    writeEnd
                }
            }
            write(setState(0))
            writeElse
            write(setState(state))
            write(ret(0))
            writeEnd
        }
    }

}
