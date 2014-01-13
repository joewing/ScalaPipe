

package autopipe

import scala.collection.mutable.ListBuffer

private[autopipe] object TypeChecker {

    private val primativeTypes = Array(
        ValueType.unsigned8,
        ValueType.signed8,
        ValueType.unsigned16,
        ValueType.signed16,
        ValueType.unsigned32,
        ValueType.signed32,
        ValueType.unsigned64,
        ValueType.signed64,
        ValueType.float32,
        ValueType.float64,
        ValueType.float96
    )

    def check(co: CodeObject, root: ASTNode): ASTNode = {
        val checker = new TypeChecker(co)
        checker.check(root, false)
    }

    def widestType(a: ValueType, b: ValueType): ValueType = {
        val aindex = primativeTypes.indexOf(a)
        val bindex = primativeTypes.indexOf(b)
        if (aindex >= 0 && bindex >= 0) {
            if (aindex > bindex) {
                return a
            } else {
                return b
            }
        } else if (a.isInstanceOf[PointerValueType] && bindex >= 0) {
            return a
        } else if (b.isInstanceOf[PointerValueType] && aindex >= 0) {
            return b
        } else if (a.isInstanceOf[FixedValueType] && bindex >= 0) {
            return a
        } else if (b.isInstanceOf[FixedValueType] && aindex >= 0) {
            return b
        } else {
            if (a != b) {
                return null
            } else {
                return a
            }
        }
    }

}

private[autopipe] class TypeChecker(co: CodeObject) {

    private def getWidestType(an: ASTNode, b: ValueType): ValueType = {
        val a = TypeChecker.widestType(getType(an), b)
        if (a != null) {
            a
        } else {
            Error.raise("type mismatch: " + a + " vs " + b, an)
            ValueType.void
        }
    }

    private def getWidestType(an: ASTNode, bn: ASTNode): ValueType = {
        if (an.isInstanceOf[Literal] && !bn.isInstanceOf[Literal]) {
            getType(bn)
        } else if (!an.isInstanceOf[Literal] && bn.isInstanceOf[Literal]) {
            getType(an)
        } else {
            getWidestType(an, getType(bn))
        }
    }

    private def getOpType(node: ASTOpNode): ValueType = node.op match {
        case NodeType.addr      => ValueType.getPointer(getType(node.a))
        case NodeType.sizeof    => ValueType.unsigned32
        case NodeType.neg       => getType(node.a)
        case NodeType.compl     => getType(node.a)
        case NodeType.and       => getWidestType(node.a, node.b)
        case NodeType.or        => getWidestType(node.a, node.b)
        case NodeType.xor       => getWidestType(node.a, node.b)
        case NodeType.shr       => getWidestType(node.a, node.b)
        case NodeType.shl       => getWidestType(node.a, node.b)
        case NodeType.add       => getWidestType(node.a, node.b)
        case NodeType.sub       => getWidestType(node.a, node.b)
        case NodeType.mul       => getWidestType(node.a, node.b)
        case NodeType.div       => getWidestType(node.a, node.b)
        case NodeType.mod       => getWidestType(node.a, node.b)
        case NodeType.not       => getType(node.a); ValueType.bool
        case NodeType.land      =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.lor       =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.eq        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.ne        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.gt        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.lt        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.ge        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.le        =>
            getWidestType(node.a, node.b); ValueType.bool
        case NodeType.abs       => getType(node.a)
        case NodeType.exp       => getType(node.a)
        case NodeType.log       => getType(node.a)
        case NodeType.sqrt      => getType(node.a)
        case NodeType.avail     => getType(node.a); ValueType.unsigned32
        case _                  => sys.error("internal: " + node)
    }

    def getType(node: ASTNode): ValueType = {

        def getSymbolType(sn: ASTSymbolNode): ValueType = {
            val stype = co.getType(sn)
            if (sn.index != null) {
                val lit: SymbolLiteral = sn.index match {
                    case sl: SymbolLiteral => sl
                    case _ => null
                }
                stype match {
                    case at: ArrayValueType =>
                        at.itemType
                    case st: StructValueType =>
                        if (lit != null) {
                            st.fields(lit.symbol)
                        } else {
                            Error.raise("invalid structure member", node)
                            ValueType.void
                        }
                    case ut: UnionValueType =>
                        if (lit != null) {
                            ut.fields(lit.symbol)
                        } else {
                            Error.raise("invalid union member", node)
                            ValueType.void
                        }
                    case nt: NativeValueType =>
                        if (lit != null) {
                            ValueType.any
                        } else {
                            Error.raise("invalid native structure member", node)
                            ValueType.void
                        }
                    case _ =>
                        Error.raise("not an array or structure", node)
                        ValueType.void
                }
            } else {
                stype
            }
        }

        def get() = node match {
            case an: ASTAssignNode      => ValueType.void
            case wn: ASTWhileNode       => ValueType.void
            case in: ASTIfNode          => ValueType.void
            case sw: ASTSwitchNode      => ValueType.void
            case hn: ASTStopNode        => ValueType.void
            case bn: ASTBlockNode       => ValueType.void
            case op: ASTOpNode          => getOpType(op)
            case li: Literal            => li.valueType
            case an: ASTAvailableNode   => ValueType.bool
            case cn: ASTConvertNode     => cn.valueType
            case sn: ASTSymbolNode      => getSymbolType(sn)
            case cn: ASTCallNode        => cn.returnType
            case rn: ASTReturnNode      => ValueType.void
            case sp: ASTSpecial         => sp.valueType
            case _ => sys.error("internal: " + node)
        }

        if (node.valueType == ValueType.void) {
            node.valueType = get()
        }
        node.valueType
    }

    private def widen(an: ASTNode,
                      b: ValueType,
                      lhs: Boolean): ASTNode = {
        val a = getType(an)
        if (a != b) {
            ASTConvertNode(check(an, lhs), b)
        } else {
            check(an, lhs)
        }
    }

    private def checkAssign(node: ASTAssignNode, lhs: Boolean): ASTNode = {

        val dest = check(node.dest, true)
        val destType = getType(dest)
        val src = check(node.src, false)

        if (lhs) {
            Error.raise("multiple assignment not allowed", node)
        }
        dest match {
            case sn: ASTSymbolNode =>
                if (co.isInput(sn)) {
                    Error.raise("assignment to input port not allowed", node)
                }
            case _ =>
                Error.raise("invalid assignment", node)
        }

        ASTAssignNode(dest, widen(src, destType, lhs))
    }

    private def checkCondition(node: ASTNode): ASTNode = {
        val cond = check(node, false)
        if (getType(cond) != ValueType.bool) {
            ASTConvertNode(cond, ValueType.bool)
        } else {
            cond
        }
    }

    private def checkIf(node: ASTIfNode, lhs: Boolean): ASTNode = {
        val cond = checkCondition(node.cond)
        val iTrue = check(node.iTrue, false)
        val iFalse = check(node.iFalse, false)
        if (lhs) {
            Error.raise("assignment to 'if'", node)
        }
        ASTIfNode(cond, iTrue, iFalse)
    }

    private def checkSwitch(node: ASTSwitchNode, lhs: Boolean): ASTNode = {
        val cond = check(node.cond, false)
        if (lhs) {
            Error.raise("assignment to 'switch'", node)
        }
        val newNode = ASTSwitchNode(cond)
        newNode.cases ++= node.cases.map { old =>
            if (old._1 != null) {
                (widen(old._1, cond.valueType, false), check(old._2, false))
            } else {
                (null, check(old._2, false))
            }
        }
        newNode
    }

    private def checkWhile(node: ASTWhileNode, lhs: Boolean): ASTNode = {
        val cond = checkCondition(node.cond)
        val body = check(node.body, false)
        if (lhs) {
            Error.raise("assignment to 'while'", node)
        }
        ASTWhileNode(cond, body)
    }

    private def checkBlock(node: ASTBlockNode, lhs: Boolean): ASTNode = {
        val children = new ListBuffer[ASTNode]
        node.children.foreach { children += check(_, false) }
        if (lhs) {
            Error.raise("invalid assignment", node)
        }
        ASTBlockNode(children)
    }

    private def checkSymbol(node: ASTSymbolNode, lhs: Boolean): ASTNode = {
        if (!lhs && co.isOutput(node)) {
            Error.raise("reading from output port not allowed", node)
        }
        val result = ASTSymbolNode(node.symbol)
        if (node.index != null) {
            val temp = check(node.index, false)
            if (temp.valueType.isInstanceOf[IntegerValueType]) {
                result.index = temp
            } else if (node.index.isInstanceOf[SymbolLiteral]) {
                result.index = temp
            } else {
                result.index = ASTConvertNode(temp, ValueType.signed32)
            }
        }
        result
    }

    private def checkCall(node: ASTCallNode, lhs: Boolean): ASTNode = {
        val result = ASTCallNode(node.func)
        val children = new ListBuffer[ASTNode]
        node.children.foreach { children += check(_, false) }
        result.apply(children: _*)
        if (lhs) {
            Error.raise("assignment to function call", node)
        }
        result
    }

    private def checkSpecial(node: ASTSpecial, lhs: Boolean): ASTNode = {
        val result = ASTSpecial(node.obj, node.method)
        result.args = node.children.map(check(_, false))
        result.valueType = node.valueType
        if (lhs) {
            Error.raise("invalid assignment", node)
        }
        result
    }

    private def widen(op: ASTOpNode, lhs: Boolean): ASTOpNode = {
        val a = check(op.a, lhs)
        val b = check(op.b, lhs)
        val widestType = getWidestType(a, b)
        val newa = widen(a, widestType, lhs)
        val newb = widen(b, widestType, lhs)
        ASTOpNode(op.op, newa, newb)
    }

    private def checkOp(node: ASTOpNode,
                        lhs: Boolean): ASTOpNode = node.op match {
        case NodeType.addr      => ASTOpNode(NodeType.addr,
                                             check(node.a, lhs))
        case NodeType.sizeof    => ASTOpNode(NodeType.sizeof,
                                             check(node.a, lhs))
        case NodeType.neg       => ASTOpNode(NodeType.neg,
                                             check(node.a, lhs))
        case NodeType.compl     => ASTOpNode(NodeType.compl,
                                             check(node.a, lhs))
        case NodeType.and       => widen(node, lhs)
        case NodeType.or        => widen(node, lhs)
        case NodeType.xor       => widen(node, lhs)
        case NodeType.shr       => widen(node, lhs)
        case NodeType.shl       => widen(node, lhs)
        case NodeType.add       => widen(node, lhs)
        case NodeType.sub       => widen(node, lhs)
        case NodeType.mul       => widen(node, lhs)
        case NodeType.div       => widen(node, lhs)
        case NodeType.mod       => widen(node, lhs)
        case NodeType.not       => ASTOpNode(NodeType.not,
                                             check(node.a, lhs))
        case NodeType.land      => widen(node, lhs)
        case NodeType.lor       => widen(node, lhs)
        case NodeType.eq        => widen(node, lhs)
        case NodeType.ne        => widen(node, lhs)
        case NodeType.gt        => widen(node, lhs)
        case NodeType.lt        => widen(node, lhs)
        case NodeType.ge        => widen(node, lhs)
        case NodeType.le        => widen(node, lhs)
        case NodeType.abs       => ASTOpNode(NodeType.abs,
                                             check(node.a, lhs))
        case NodeType.exp       => ASTOpNode(NodeType.exp,
                                             check(node.a, lhs))
        case NodeType.log       => ASTOpNode(NodeType.log,
                                             check(node.a, lhs))
        case NodeType.sqrt      => ASTOpNode(NodeType.sqrt,
                                             check(node.a, lhs))
        case NodeType.avail     => ASTOpNode(NodeType.avail,
                                             check(node.a, lhs))
        case _                  => sys.error("internal: " + node)
    }

    private def checkConvert(node: ASTConvertNode,
                             lhs: Boolean): ASTConvertNode = {
        val a = check(node.a, lhs)
        if (lhs) {
            Error.raise("invalid assignment", node)
        }
        if (a.valueType.eq(ValueType.any)) {
            a.valueType = a match {
                case it: IntLiteral      => ValueType.signed64
                case ft: FloatLiteral    => ValueType.float64
                case _                        => node.valueType
            }
        }
        ASTConvertNode(a, node.valueType)
    }

    private def checkReturn(node: ASTReturnNode,
                            lhs: Boolean): ASTReturnNode = {
        if (lhs) {
            Error.raise("invalid assignment", node)
        }
        ASTReturnNode(check(node.a, false))
    }

    private def checkLiteral(node: Literal, lhs: Boolean): Literal = {
        if (lhs) {
            Error.raise("assignment to literal not allowed", node)
        }
        node
    }

    def check(node: ASTNode, lhs: Boolean): ASTNode = {
        val newNode = node match {
            case an: ASTAssignNode      => checkAssign(an, lhs)
            case in: ASTIfNode          => checkIf(in, lhs)
            case sw: ASTSwitchNode      => checkSwitch(sw, lhs)
            case wn: ASTWhileNode       => checkWhile(wn, lhs)
            case sn: ASTStopNode        => sn
            case bn: ASTBlockNode       => checkBlock(bn, lhs)
            case cn: ASTCallNode        => checkCall(cn, lhs)
            case sn: ASTSymbolNode      => checkSymbol(sn, lhs)
            case on: ASTOpNode          => checkOp(on, lhs)
            case cn: ASTConvertNode     => checkConvert(cn, lhs)
            case av: ASTAvailableNode   => av
            case rn: ASTReturnNode      => checkReturn(rn, lhs)
            case sp: ASTSpecial         => checkSpecial(sp, lhs)
            case null                   => null
            case li: Literal            => checkLiteral(li, lhs)
            case _                      => sys.error("internal: " + node)
        }
        if (newNode != null) {
            newNode.fileName = node.fileName
            newNode.lineNumber = node.lineNumber
            newNode.valueType = getType(newNode)
        }
        newNode
    }

}

