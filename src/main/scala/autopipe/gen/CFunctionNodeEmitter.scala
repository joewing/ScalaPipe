
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
        if (node.index == null) {
            name
        } else if (isNative(node.valueType)) {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                name + "." + node.index.toString
            } else {
                name + "[" + emitExpr(node.index) + "]"
            }
        } else if (isNativePointer(node.valueType)) {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                name + "->" + node.index.toString
            } else {
                "(*" + name + ")[" + emitExpr(node.index) + "]"
            }
        } else {
            if (node.index.isInstanceOf[SymbolLiteral]) {
                name + "." + node.index.toString
            } else {
                name + ".values[" + emitExpr(node.index) + "]"
            }
        }
    }

    override def emitAssign(node: ASTAssignNode) {
        write(emitExpr(node.dest) + " = " + emitExpr(node.src) + ";")
        updateClocks(getTiming(node))
    }

    override def emitStop(node: ASTStopNode) {
        Error.raise("stop not valid in a function")
    }

    override def emitReturn(node: ASTReturnNode) {
        updateClocks(getTiming(node))
        if (node.a != null) {
            write("return " + emitExpr(node.a) + ";")
        } else {
            write("return;")
        }
    }

    override def updateClocks(count: Int) {
        if (count > 0) {
            write("*clocks += " + count + ";")
        }
    }

    override def checkInputs(node: ASTNode): Int = 0

    override def releaseInputs(node: ASTNode, state: Int) {
    }

}

