package scalapipe.gen

import scalapipe._

private[scalapipe] abstract class CNodeEmitter(
        _kt: KernelType,
        val timing: Map[ASTNode, Int]
    ) extends NodeEmitter(_kt) with CGenerator {

    private var usedTimings = Set[ASTNode]()

    def emitAvailable(node: ASTAvailableNode): String
    def emitSymbol(node: ASTSymbolNode): String
    def emitSymbolBase(node: ASTSymbolNode): String

    def emitAssign(node: ASTAssignNode): Unit
    def emitStop(node: ASTStopNode)
    def emitReturn(node: ASTReturnNode)
    def updateClocks(node: ASTNode)

    private def emitBinaryOp(op: String, node: ASTOpNode): String =
        "(" + emitExpr(node.a) + ") " + op + " (" + emitExpr(node.b) + ")"

    private def emitUnaryOp(op: String, node: ASTOpNode): String =
        op + "(" + emitExpr(node.a) + ")"

    private def emitFixedOp(node: ASTOpNode,
                            frac: Int): String = node.op match {
        case NodeType.neg       => emitUnaryOp("-", node)
        case NodeType.compl     => emitUnaryOp("~", node)
        case NodeType.addr      => emitUnaryOp("&", node)
        case NodeType.sizeof    => emitUnaryOp("sizeof", node)
        case NodeType.and       => emitBinaryOp("&", node)
        case NodeType.or        => emitBinaryOp("|", node)
        case NodeType.xor       => emitBinaryOp("^", node)
        case NodeType.shr       => emitBinaryOp(">>", node)
        case NodeType.shl       => emitBinaryOp("<<", node)
        case NodeType.add       => emitBinaryOp("+", node)
        case NodeType.sub       => emitBinaryOp("-", node)
        case NodeType.mul       =>
            val f1 = frac / 2
            val f2 = (frac + 1) / 2
            "(((" + emitExpr(node.a) + ") >> " + f1 + ") * ((" +
                emitExpr(node.b) + ") >> " + f2 + "))"
        case NodeType.div       =>
            val f1 = frac / 2
            val f2 = (frac + 1) / 2
            "((" + emitExpr(node.a) + " / (" +
                emitExpr(node.b) + " >> " + f1 + ")) << " + f2 + ")"
        case _ =>
            Error.raise("invalid fixed point operation: " + node, node)
    }

    private def emitFunctionOp(name: String, node: ASTOpNode) = {
        val expr = emitExpr(node.a)
        node.valueType match {
            case ValueType.unsigned8    => s"sp_${name}8($expr)"
            case ValueType.signed8      => s"sp_${name}8($expr)"
            case ValueType.unsigned16   => s"sp_${name}16($expr)"
            case ValueType.signed16     => s"sp_${name}16($expr)"
            case ValueType.unsigned32   => s"sp_${name}32($expr)"
            case ValueType.signed32     => s"sp_${name}32($expr)"
            case ValueType.unsigned64   => s"sp_${name}64($expr)"
            case ValueType.signed64     => s"sp_${name}64($expr)"
            case ValueType.float32      => s"${name}f($expr)"
            case ValueType.float64      => s"${name}($expr)"
            case ValueType.float96      => s"${name}l($expr)"
            case _                      =>
                Error.raise("invalid op type", node)
        }
    }

    private def emitRegularOp(node: ASTOpNode): String = node.op match {
        case NodeType.not       => emitUnaryOp("!", node)
        case NodeType.neg       => emitUnaryOp("-", node)
        case NodeType.compl     => emitUnaryOp("~", node)
        case NodeType.addr      => emitUnaryOp("&", node)
        case NodeType.sizeof    => emitUnaryOp("sizeof", node)
        case NodeType.land      => emitBinaryOp("&&", node)
        case NodeType.lor       => emitBinaryOp("||", node)
        case NodeType.and       => emitBinaryOp("&", node)
        case NodeType.or        => emitBinaryOp("|", node)
        case NodeType.xor       => emitBinaryOp("^", node)
        case NodeType.shr       => emitBinaryOp(">>", node)
        case NodeType.shl       => emitBinaryOp("<<", node)
        case NodeType.add       => emitBinaryOp("+", node)
        case NodeType.sub       => emitBinaryOp("-", node)
        case NodeType.mul       => emitBinaryOp("*", node)
        case NodeType.div       => emitBinaryOp("/", node)
        case NodeType.mod       => emitBinaryOp("%", node)
        case NodeType.eq        => emitBinaryOp("==", node)
        case NodeType.ne        => emitBinaryOp("!=", node)
        case NodeType.gt        => emitBinaryOp(">", node)
        case NodeType.lt        => emitBinaryOp("<", node)
        case NodeType.ge        => emitBinaryOp(">=", node)
        case NodeType.le        => emitBinaryOp("<=", node)
        case NodeType.abs       =>
            val expr = emitExpr(node.a)
            s"($expr) < 0 ? -($expr) : ($expr)"     // FIXME
        case NodeType.exp       => emitFunctionOp("exp", node)
        case NodeType.log       => emitFunctionOp("log", node)
        case NodeType.sqrt      => emitFunctionOp("sqrt", node)
        case NodeType.sin       => emitFunctionOp("sin", node)
        case NodeType.cos       => emitFunctionOp("cos", node)
        case NodeType.tan       => emitFunctionOp("tan", node)
        case _ =>
            Error.raise("invalid operation", node)
    }

    private def emitArrayOp(node: ASTOpNode): String = node.op match {
        case NodeType.addr      => emitUnaryOp("&", node)
        case NodeType.sizeof    => emitUnaryOp("sizeof", node)
        case _ =>
            Error.raise(s"invalid operation: $node", node)
    }

    private def emitPointerOp(node: ASTOpNode): String = node.op match {
        case NodeType.addr      => emitUnaryOp("&", node)
        case NodeType.sizeof    => emitUnaryOp("sizeof", node)
        case _ =>
            Error.raise(s"invalid operation: $node", node)
    }

    private def emitOp(node: ASTOpNode): String = node.valueType match {
        case f: FixedValueType              => emitFixedOp(node, f.fraction)
        case i: IntegerValueType            => emitRegularOp(node)
        case f: FloatValueType              => emitRegularOp(node)
        case a: ArrayValueType              => emitArrayOp(node)
        case p: PointerValueType            => emitPointerOp(node)
        case _ =>
            Error.raise(s"invalid operation: $node, valueType: " +
                        node.valueType.getClass, node)
    }

    private def emitCall(node: ASTCallNode): String = {
        val argString = node.args.map(emitExpr).mkString(", ")
        if (kt.isInternal(node.func) && kt.parameters.get('profile)) {
            if (argString.isEmpty) {
                s"${node.symbol}(&kernel->sp_clocks)"
            } else {
                s"${node.symbol}(&kernel->sp_clocks, $argString)"
            }
        } else {
            s"${node.symbol}($argString)"
        }
    }

    private def emitLiteral(l: Literal): String = l.toString

    private def emitConvert(node: ASTConvertNode): String = {
        val subexpr = emitExpr(node.a)
        val destType = node.valueType
        val srcType = node.a.valueType
        (srcType, destType) match {
            case (at: IntegerValueType, bt: IntegerValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ")"
            case (at: FloatValueType, bt: FloatValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ")"
            case (at: IntegerValueType, bt: FloatValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ")"
            case (at: FloatValueType, bt: IntegerValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ")"

            case (at: IntegerValueType, bt: FixedValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ") << " + bt.fraction
            case (at: FixedValueType, bt: IntegerValueType) =>
                "(" + bt.baseType + ")((" + subexpr + ") >> " + at.fraction + ")"

            case (at: FloatValueType, bt: FixedValueType) =>
                "(" + bt.baseType + ")((" + subexpr + ") * " +
                    (1 << bt.fraction) + ")"
            case (at: FixedValueType, bt: FloatValueType) =>
                "(" + bt.baseType + ")(" + subexpr + ") / " +
                (1L << at.fraction)

            case (at: FixedValueType, bt: FixedValueType) =>
                if (at.fraction > bt.fraction) {
                    val shift = at.fraction - bt.fraction
                    "(" + bt.baseType + ")((" + subexpr + ") >> " + shift + ")"
                } else if (at.fraction < bt.fraction) {
                    val shift = bt.fraction - at.fraction
                    "(" + bt.baseType + ")((" + subexpr + ") << " + shift + ")"
                } else {
                    subexpr
                }

            case (at: IntegerValueType, bt: PointerValueType) =>
                subexpr

            case _ =>
                Error.raise("invalid (ast) conversion from " + srcType +
                            " to " + destType, node)
        }
    }

    protected def emitExpr(node: ASTNode): String = node match {
        case l: Literal             => emitLiteral(l)
        case s: ASTSymbolNode       => emitSymbol(s)
        case c: ASTCallNode         => emitCall(c)
        case o: ASTOpNode           => emitOp(o)
        case a: ASTAvailableNode    => emitAvailable(a)
        case c: ASTConvertNode      => emitConvert(c)
        case null                   => "0"
        case _ => Error.raise("invalid expression", node)
    }

    private def emitIf(node: ASTIfNode) {
        writeIf(emitExpr(node.cond))
        emit(node.iTrue)
        if (node.iFalse != null) {
            writeElse
            emit(node.iFalse)
        }
        writeEnd
    }

    private def emitSwitch(node: ASTSwitchNode) {
        val test = emitExpr(node.cond)
        var first = true
        node.cases.foreach { c =>
            if (c._1 != null) {
                val cond = emitExpr(c._1)
                val expr = s"($test) == ($cond)"
                if (first) {
                    writeIf(expr)
                    first = false
                } else {
                    writeElseIf(expr)
                }
                emit(c._2)
            }
        }
        node.cases.find({ c => c._1 == null }) match {
            case Some((_, b)) =>
                if (first) {
                    emit(b)
                    first = false
                } else {
                    writeElse
                    emit(b)
                }
            case None => ()
        }
        if (!first) {
            writeEnd
        }
    }

    private def emitWhile(node: ASTWhileNode) {
        writeWhile(emitExpr(node.cond))
        emit(node.body)
        writeEnd
    }

    private def emitCallProc(node: ASTCallNode) {
        write(emitCall(node) + ";")
    }

    private def emitBlock(node: ASTBlockNode) {
        for (n <- node.children) {
            emit(n)
        }
    }

    protected def getTiming(node: ASTNode): Int = {
        if (timing != null && node != null && !usedTimings.contains(node)) {
            usedTimings += node
            return timing.getOrElse(node, 0)
        } else {
            return 0
        }
    }

    def emit(node: ASTNode) {
        updateClocks(node)
        node match {
            case anode: ASTAssignNode   => emitAssign(anode)
            case cond:  ASTIfNode       => emitIf(cond)
            case sw:    ASTSwitchNode   => emitSwitch(sw)
            case loop:  ASTWhileNode    => emitWhile(loop)
            case call:  ASTCallNode     => emitCallProc(call)
            case stop:  ASTStopNode     => emitStop(stop)
            case block: ASTBlockNode    => emitBlock(block)
            case ret:   ASTReturnNode   => emitReturn(ret)
            case null                   => ()
            case _                      =>
                Error.raise("invalid start statement: " + node, node)
        }
    }

}
