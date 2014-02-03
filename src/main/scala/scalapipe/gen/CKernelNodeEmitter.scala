package scalapipe.gen

import scalapipe._

private[scalapipe] class CKernelNodeEmitter(
        _kt: InternalKernelType,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(_kt, _timing) with ASTUtils {

    override def emitAvailable(node: ASTAvailableNode): String = {
        val name = node.symbol
        if (kt.isInput(name)) {
            val index = kt.inputIndex(name)
            s"sp_get_available(kernel, $index)"
        } else if (kt.isOutput(name)) {
            val index = kt.outputIndex(name)
            s"sp_get_free(kernel, $index)"
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

    private def emitSymbolBase(node: ASTSymbolNode): String = {
        val name = node.symbol
        if (kt.isLocal(name)) {
            return s"$name"
        } else if (kt.isInput(name)) {
            val index = kt.inputIndex(name)
            return s"sp_read_input$index(kernel)"
        } else if (kt.isOutput(name)) {
            return s"*$name"
        } else if (kt.isState(name) || kt.isConfig(name)) {
            return s"kernel->$name"
        } else {
            Error.raise(s"symbol not declared: $name", node)
        }
    }

    override def emitSymbol(node: ASTSymbolNode): String = {
        val base = emitSymbolBase(node)
        val valueType = kt.getType(node)
        val start = ((base, valueType))
        val result = node.indexes.foldLeft(start) { (a, index) =>
            val (base, vt) = a
            emitComponent(base, vt, index)
        }
        result._1
    }

    private def getOffset(node: ASTSymbolNode): String = {
        val location = "(char*)&" + emitSymbol(node)
        val base = "(char*)&" + emitSymbolBase(node)
        val baseOffset = kt.getBaseOffset(node.symbol)
        return s"((unsigned)($location - $base) + $baseOffset)"
    }

    override def emitAssign(node: ASTAssignNode) {
        val outputs = localOutputs(node)
        for (o <- outputs) {
            val oindex = kt.outputIndex(o)
            val vtype = kt.outputs(oindex).valueType.name
            write(s"$o = ($vtype*)sp_allocate(kernel, $oindex);")
        }

        // TODO: output trace data for reads.

        val dest = emitExpr(node.dest)
        val src = emitExpr(node.src)
        write(s"$dest = $src;")
        updateClocks(getTiming(node))

        if (kt.parameters.get[Boolean]('trace) && !node.dest.valueType.flat) {
            val offsetStr = getOffset(node.dest)
            val size = node.src.valueType.bytes.toHexString
            write("fprintf(kernel->trace_fd, \"W%x:" + size +
                  "\\n\", " + offsetStr + ");")
        }

        for (oindex <- outputs.map(kt.outputIndex(_))) {
            if (kt.parameters.get[Boolean]('trace)) {
                write("fprintf(kernel->trace_fd, \"P" + oindex + "\\n\");")
            }
            write(s"sp_send(kernel, $oindex);")
        }
    }

    override def emitStop(node: ASTStopNode) {
        updateClocks(getTiming(node))
        write(s"return;")
    }

    override def emitReturn(node: ASTReturnNode) {
        val name = kt.outputs(0).name
        val vtype = kt.outputs(0).valueType.name
        val src = emitExpr(node.a)
        write(s"$name = ($vtype*)sp_allocate(kernel, 0);")
        write(s"*$name = $src;")
        updateClocks(getTiming(node))
        write(s"sp_send(kernel, 0);")
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write(s"kernel->sp_clocks += $count;")
        }
    }

}
