
package autopipe.gen

import autopipe._

private[autopipe] class CBlockNodeEmitter(
        val bt: InternalBlockType,
        val gen: StateTrait,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(bt, _timing) with CLike {

    private def setState(state: Int): String =
        "block->ap_state_index = " + state + ";"

    private def release(name: String, index: Int, count: Int): String = {
        "ap_release(block, " + index + ", " + count + "); " +
        name + " = NULL;"
    }

    private def ret(result: Int): String = {
        "return " + result + ";"
    }

    override def emitAvailable(node: ASTAvailableNode): String = {
        val name = node.symbol
        if (bt.isInput(name)) {
            val index = bt.inputIndex(name)
            "(" + name + " != 0)"
        } else if (bt.isOutput(name)) {
            val index = bt.outputIndex(name)
            "ap_get_free(block, " + index + ")"
        } else {
            Error.raise("argument to avail must be an input or output", node)
        }
    }

    override def emitSymbol(node: ASTSymbolNode): String = {

        def isNative(vt: ValueType) = vt.isInstanceOf[NativeValueType]

        def isNativePointer(vt: ValueType) = vt match {
            case p: PointerValueType if isNative(p.itemType) => true
            case _ => false
        }

        val name = node.symbol
        if (node.index == null) {
            if (bt.isInput(name)) {
                "*" + name
            } else if (bt.isOutput(name)) {
                "*" + name
            } else if (bt.isLocal(name)) {
                name
            } else if (bt.isState(name) || bt.isConfig(name)) {
                "block->" + name
            } else {
                Error.raise("symbol not declared: " + name, node)
            }
        } else if (isNative(node.valueType)) {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                val indexString = node.index.toString
                if (bt.isInput(name) || bt.isOutput(name)) {
                    name + "." + indexString
                } else if (bt.isLocal(name)) {
                    name + "." + indexString
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "block->" + name + "." + indexString
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            } else {
                val indexString = emitExpr(node.index)
                if (bt.isInput(name) || bt.isOutput(name)) {
                    name + "[" + indexString + "]"
                } else if (bt.isLocal(name)) {
                    name + "[" + indexString + "]"
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "block->" + name + "[" + indexString + "]"
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            }
        } else if (isNativePointer(node.valueType)) {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                val indexString = node.index.toString
                if (bt.isInput(name) || bt.isOutput(name)) {
                    name + "->" + indexString
                } else if (bt.isLocal(name)) {
                    name + "->" + indexString
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "block->" + name + "->" + indexString
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            } else {
                val indexString = emitExpr(node.index)
                if (bt.isInput(name) || bt.isOutput(name)) {
                    "(*" + name + ")[" + indexString + "]"
                } else if (bt.isLocal(name)) {
                    "(*" + name + ")[" + indexString + "]"
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "(*block->" + name + ")[" + indexString + "]"
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            }
        } else {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                val indexString = emitExpr(node.index)
                if (bt.isInput(name) || bt.isOutput(name)) {
                    name + "." + indexString
                } else if (bt.isLocal(name)) {
                    name + "." + indexString
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "block->" + name + "." + indexString
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            } else {
                val indexString = emitExpr(node.index)
                if (bt.isInput(name) || bt.isOutput(name)) {
                    name + ".values[" + indexString + "]"
                } else if (bt.isLocal(name)) {
                    name + ".values[" + indexString + "]"
                } else if (bt.isState(name) || bt.isConfig(name)) {
                    "block->" + name + ".values[" + indexString + "]"
                } else {
                    Error.raise("symbol not declared: " + name, node)
                }
            }
        }
    }

    override def emitAssign(node: ASTAssignNode) {

        var outputs = getLocalOutputs(node)
        for (o <- outputs) {
            val oindex = bt.outputIndex(o)
            val valueType = bt.outputs(oindex).valueType
            write(o + " = (" + valueType.name + "*)ap_allocate(block, " +
                  oindex + ", 1);")
        }

        write(emitExpr(node.dest) + " = " + emitExpr(node.src) + ";")

        updateClocks(getTiming(node))

        for (o <- outputs) {
            val oindex = bt.outputIndex(o)
            write("ap_send(block, " + oindex + ", 1);")
        }

    }

    override def emitStop(node: ASTStopNode) {
        updateClocks(getTiming(node))
        write(setState(-1))
        write(ret(1))
    }

    override def emitReturn(node: ASTReturnNode) {
        val valueType = bt.outputs(0).valueType
        write("output = (" + valueType.name + "*)ap_allocate(block, 0, 1);")
        write("*output = " + emitExpr(node.a) + ";")
        updateClocks(getTiming(node))
        write("ap_send(block, 0, 1);")
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write("block->ap_clocks += " + count + ";")
        }
    }

    override def checkInputs(node: ASTNode): Int = {

        def emitAccess(read: Boolean, name: String, index: ASTNode) {
            bt.getSymbol(name) match {
                case is: InputSymbol =>
                    assert(read)
                    write("fprintf(block->trace_fd, \"C" + is.index + "\\n\");")
                case os: OutputSymbol =>
                    assert(!read)
                    write("fprintf(block->trace_fd, \"P" + os.index + "\\n\");")
                case s: BaseSymbol =>
                    s.valueType match {
                        case at: ArrayValueType if at.bits >= 1024 =>
                            val offset = bt.getBaseOffset(name)
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
        val portsToCheck = getBlockingInputs(node).filter { !isCheckedPort(_) }
        if (!portsToCheck.isEmpty)  {
            addCheckedPorts(portsToCheck)
            writeLeft("AP_STATE_" + gen.nextState + ":")
            write("if(" + portsToCheck.map(_ + " != NULL").mkString(" && ") +
                  ") {")
            enter
        }
        if (bt.parameters.get('trace)) {
            for ((r, index) <- getReads(node)) {
                emitAccess(true, r, index)
            }
            for ((w, index) <- getWrites(node)) {
                emitAccess(false, w, index)
            }
        }
        gen.currentState
    }

    override def releaseInputs(node: ASTNode, state: Int) {
        val portsToRelease = getCheckedPorts
        endScope
        val inputs = bt.inputs.map({ _.name }).filter({ !isCheckedPort(_) })
        if (!portsToRelease.isEmpty) {
            for (i <- inputs) {
                val index = bt.inputIndex(i)
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

