package autopipe.gen

import autopipe._

private[autopipe] class CFunctionNodeEmitter(
        val ft: InternalFunctionType,
        _timing: Map[ASTNode, Int]
    ) extends CNodeEmitter(ft, _timing) {

    override def emitAvailable(node: ASTAvailableNode): String = "1"

    override def emitSymbol(node: ASTSymbolNode): String = {

        def isNative(vt: ValueType) = vt.isInstanceOf[NativeValueType]

        def isNativePointer(vt: ValueType) = vt match {
            case p: PointerValueType if isNative(p.itemType) => true
            case _ => false
        }

        val name = node.symbol
        val index = node.index
        if (index == null) {
            return name
        }

        val expr = index match {
            case sl: SymbolLiteral  => "." + index
            case _                  => "[" + emitExpr(index) + "]"
        }

        if (isNative(node.valueType)) {
            return s"$name$index"
        } else if (isNativePointer(node.valueType)) {
            return s"(*$name)$index"
        } else if (index.isInstanceOf[SymbolLiteral]) {
            return s"$name.$index"
        } else {
            return s"$name.values$expr"
        }

    }

    override def emitAssign(node: ASTAssignNode) {
        val dest = emitExpr(node.dest)
        val src = emitExpr(node.src)
        write(s"$dest = $src;")
        updateClocks(getTiming(node))
    }

    override def emitStop(node: ASTStopNode) {
        Error.raise("stop not valid in a function")
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

    override def checkInputs(node: ASTNode): Int = 0

    override def releaseInputs(node: ASTNode, state: Int) {
    }

}
