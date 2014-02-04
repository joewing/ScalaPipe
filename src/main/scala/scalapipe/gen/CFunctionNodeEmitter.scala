package scalapipe.gen

import scalapipe._

private[scalapipe] class CFunctionNodeEmitter(
        val ft: InternalFunctionType,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(ft, _timing) with ASTUtils {

    override def emitAvailable(node: ASTAvailableNode): String = "1"

    private def emitComponent(base: String,
                              vt: ValueType,
                              comp: ASTNode): (String, ValueType) = {
        val expr = comp match {
            case sl: SymbolLiteral  => "." + comp
            case _                  => "[" + emitExpr(comp) + "]"
        }
        val nvt = vt match {
            case at: ArrayValueType     => at.itemType
            case rt: RecordValueType    => rt.fieldType(comp)
            case nt: NativeValueType    => ValueType.any
            case _                      => ValueType.void
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

    override def emitSymbolBase(node: ASTSymbolNode): String = {
        node.symbol
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

    override def emitAssign(node: ASTAssignNode) {
        val src = emitExpr(node.src)
        if (kt.isOutput(node.dest)) {
            updateClocks(getTiming(node))
            writeReturn(src)
        } else {
            val dest = emitExpr(node.dest)
            write(s"$dest = $src;")
            updateClocks(getTiming(node))
        }
    }

    override def emitStop(node: ASTStopNode) {
        Error.raise("stop not valid in a function", node)
    }

    override def emitReturn(node: ASTReturnNode) {
        updateClocks(getTiming(node))
        writeReturn(emitExpr(node.a))
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write(s"*clocks += $count;")
        }
    }

}
