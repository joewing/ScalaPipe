
package scalapipe.gen

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import scalapipe._

private[scalapipe] case class IRNodeEmitter(
        _kt: KernelType
    ) extends NodeEmitter(_kt) with HDLGenerator {

    private val instructions = new ArrayBuffer[StateBlock]

    private def append(node: IRNode, ast: ASTNode): Int = {
        val l = instructions.size
        instructions += null
        set(l, node, ast)
        return l
    }

    private def set(l: Int, node: IRNode, ast: ASTNode) {
        val sb = node match {
            case c: IRConditional   => StateBlock(List(node), ast=ast, label=l)
            case g: IRGoto          => StateBlock(List(node), ast=ast, label=l)
            case s: IRSwitch        => StateBlock(List(node), ast=ast, label=l)
            case s: IRStart         => StateBlock(List(node), ast=ast, label=l)
            case s: IRStop          => StateBlock(List(node), ast=ast, label=l)
            case r: IRReturn        => StateBlock(List(node), ast=ast, label=l)
            case _                  => StateBlock(List(node, IRGoto()),
                                                  ast=ast, label=l)
        }
        instructions(l) = sb
    }

    private def getPointer(ast: ASTNode): Int = append(IRGoto(), ast)

    private def sort(op: NodeType.Value,
                     a: BaseSymbol,
                     b: BaseSymbol,
                     symmetric: NodeType.Value) = {
        if (a < b || symmetric == NodeType.invalid) {
            (op, a, b)
        } else {
            (symmetric, b, a)
        }
    }

    private def emitBinaryOp(node: ASTOpNode,
                             symmetric: NodeType.Value = NodeType.invalid) = {
        val dest = kt.createTemp(node.valueType)
        val a = emitExpr(node.a)
        val b = emitExpr(node.b)
        val (op, srca, srcb) = sort(node.op, a, b, symmetric)
        append(IRInstruction(op, dest, srca, srcb), node)
        kt.releaseTemp(srca)
        kt.releaseTemp(srcb)
        dest
    }

    private def emitAdd(ast: ASTNode,
                        a: BaseSymbol,
                        b: BaseSymbol): BaseSymbol = (a, b) match {
        case (ai: ImmediateSymbol, bi: ImmediateSymbol) =>
            emitS32(ai.value.long.toInt + bi.value.long.toInt)
        case _ =>
            val (_, srca, srcb) = sort(NodeType.add, a, b, NodeType.add)
            val dest = kt.createTemp(a.valueType)
            append(IRInstruction(NodeType.add, dest, srca, srcb), ast)
            dest
    }

    private def emitMul(ast: ASTNode,
                        a: BaseSymbol,
                        b: BaseSymbol): BaseSymbol = (a, b) match {
        case (ai: ImmediateSymbol, bi: ImmediateSymbol) =>
            emitS32(ai.value.long.toInt * bi.value.long.toInt)
        case _ =>
            val (_, srca, srcb) = sort(NodeType.mul, a, b, NodeType.mul)
            val dest = kt.createTemp(a.valueType)
            append(IRInstruction(NodeType.mul, dest, srca, srcb), ast)
            dest
    }

    private def emitUnaryOp(node: ASTOpNode): BaseSymbol = {
        val dest = kt.createTemp(node.valueType)
        val src = emitExpr(node.a)
        append(IRInstruction(node.op, dest, src), node)
        kt.releaseTemp(src)
        dest
    }

    private def emitAvailable(node: ASTAvailableNode): BaseSymbol = {
        val dest = kt.createTemp(node.valueType)
        val src = kt.getSymbol(node.symbol)
        append(IRInstruction(node.op, dest, src), node)
        kt.releaseTemp(src)
        dest
    }

    private def emitOp(node: ASTOpNode): BaseSymbol = node.op match {
        case NodeType.not   => emitUnaryOp(node)
        case NodeType.neg   => emitUnaryOp(node)
        case NodeType.compl => emitUnaryOp(node)
        case NodeType.addr  => emitUnaryOp(node)
        case NodeType.land  => emitBinaryOp(node, NodeType.land)
        case NodeType.lor   => emitBinaryOp(node, NodeType.lor)
        case NodeType.and   => emitBinaryOp(node, NodeType.and)
        case NodeType.or    => emitBinaryOp(node, NodeType.or)
        case NodeType.xor   => emitBinaryOp(node, NodeType.xor)
        case NodeType.shr   => emitBinaryOp(node)
        case NodeType.shl   => emitBinaryOp(node)
        case NodeType.add   => emitBinaryOp(node, NodeType.add)
        case NodeType.sub   => emitBinaryOp(node)
        case NodeType.mul   => emitBinaryOp(node, NodeType.mul)
        case NodeType.div   => emitBinaryOp(node)
        case NodeType.mod   => emitBinaryOp(node)
        case NodeType.eq    => emitBinaryOp(node, NodeType.eq)
        case NodeType.ne    => emitBinaryOp(node, NodeType.ne)
        case NodeType.gt    => emitBinaryOp(node, NodeType.lt)
        case NodeType.lt    => emitBinaryOp(node, NodeType.gt)
        case NodeType.ge    => emitBinaryOp(node, NodeType.le)
        case NodeType.le    => emitBinaryOp(node, NodeType.ge)
        case NodeType.abs   => emitUnaryOp(node)
        case NodeType.exp   => emitUnaryOp(node)
        case NodeType.log   => emitUnaryOp(node)
        case NodeType.sqrt  => emitUnaryOp(node)
        case NodeType.sin   => emitUnaryOp(node)
        case NodeType.cos   => emitUnaryOp(node)
        case NodeType.tan   => emitUnaryOp(node)
        case _              => sys.error("invalid operation: " + node)
    }

    private def emitCallFunc(node: ASTCallNode): BaseSymbol = {
        sys.error("not implemented")
    }

    private def emitS32(value: Int): BaseSymbol = {
        new ImmediateSymbol(Literal.get(value))
    }

    private def emitLiteral(l: Literal): BaseSymbol = new ImmediateSymbol(l)

    private def emitOffset(vt: ValueType,
                           base: Option[BaseSymbol],
                           comp: ASTNode): (Option[BaseSymbol], ValueType) = {
        val nvt = vt match {
            case at: ArrayValueType     => at.itemType
            case rt: RecordValueType    => rt.fieldType(comp)
            case nt: NativeValueType    => ValueType.any
            case _                      => ValueType.void
        }

        val expr: BaseSymbol = vt match {
            case rt: RecordValueType    => emitS32(rt.offset(comp))
            case at: ArrayValueType     =>
                val multiplier = emitS32(at.itemType.bytes)
                val temp = emitMul(comp, multiplier, emitExpr(comp))
                kt.releaseTemp(multiplier)
                temp
            case _ => emitS32(0)
        }
        val offset = base.foldLeft(expr) { (a, b) =>
            val temp = emitAdd(comp, a, b)
            kt.releaseTemp(a)
            kt.releaseTemp(b)
            temp
        }

        return (Option(offset), nvt)
    }

    private def emitOffset(node: ASTSymbolNode): BaseSymbol = {
        val valueType = kt.getType(node)
        val start = (Option.empty[BaseSymbol], valueType)
        val offset = node.indexes.foldLeft(start) { (a, index) =>
            val (base, vt) = a
            emitOffset(vt, base, index)
        }
        return offset._1.head
    }

    private def emitSymbol(node: ASTSymbolNode): BaseSymbol = {

        val sym = kt.getSymbol(node.symbol)
        var src = sym
        if (sym.isInstanceOf[PortSymbol]) {
            src = kt.createTemp(node.valueType)
            append(IRInstruction(NodeType.assign, src, sym), node)
        }

        if (node.indexes.isEmpty) {
            return src
        } else {
            val offset = emitOffset(node)
            val dest = kt.createTemp(node.valueType)
            append(IRLoad(dest, src, offset), node)
            kt.releaseTemp(src)
            kt.releaseTemp(offset)
            return dest
        }

    }

    private def emitConvert(node: ASTConvertNode): BaseSymbol = {
        val expr = emitExpr(node.a)
        val destType = node.valueType
        val srcType = node.a.valueType
        (srcType, destType) match {

            case (st: IntegerValueType, dt: IntegerValueType) =>
                if (destType.bits != srcType.bits) {
                    val dest = kt.createTemp(destType)
                    append(IRInstruction(NodeType.convert, dest, expr), node)
                    kt.releaseTemp(expr)
                    dest
                } else {
                    expr
                }
            case (st: FloatValueType, dt: FloatValueType) =>
                if (destType.bits != srcType.bits) {
                    val dest = kt.createTemp(destType)
                    append(IRInstruction(NodeType.convert, dest, expr), node)
                    kt.releaseTemp(expr)
                    dest
                } else {
                    expr
                }
            case (st: IntegerValueType, dt: FloatValueType) =>
                val dest = kt.createTemp(destType)
                append(IRInstruction(NodeType.convert, dest, expr), node)
                kt.releaseTemp(expr)
                dest
            case (st: FloatValueType, dt: IntegerValueType) =>
                val dest = kt.createTemp(destType)
                append(IRInstruction(NodeType.convert, dest, expr), node)
                kt.releaseTemp(expr)
                dest

            case (st: IntegerValueType, dt: FixedValueType) =>
                val dest = kt.createTemp(destType)
                append(IRInstruction(NodeType.shl, dest, expr,
                                            emitS32(dt.fraction)), node)
                kt.releaseTemp(expr)
                dest
            case (st: FixedValueType, dt: IntegerValueType) =>
                val dest = kt.createTemp(destType)
                append(IRInstruction(NodeType.shr, dest, expr,
                                            emitS32(st.fraction)), node)
                kt.releaseTemp(expr)
                dest

            case (st: FixedValueType, dt: FixedValueType) =>
                val dest = kt.createTemp(destType)
                if (st.fraction > dt.fraction) {
                    val shift = st.fraction - dt.fraction
                    append(IRInstruction(NodeType.shr, dest, expr,
                                                emitS32(shift)), node)
                } else if (st.fraction < dt.fraction) {
                    val shift = dt.fraction - st.fraction
                    append(IRInstruction(NodeType.shl, dest, expr,
                                                emitS32(shift)), node)
                } else {
                    append(IRInstruction(NodeType.assign, dest, expr), node)
                }
                dest

            case (st: FloatValueType, dt: FixedValueType) =>
                Error.raise("float-to-fixed conversion not supported", node)
                expr

            case (st: FixedValueType, dt: FloatValueType) =>
                Error.raise("fixed-to-float conversion not supported", node)
                expr

            case _ =>
                Error.raise("invalid (hardware) conversion from " + srcType +
                            " to " + destType, node)
                expr

        }
    }

    private def emitCall(node: ASTCallNode): BaseSymbol = {
        val dest = if (node.returnType != null) {
                kt.createTemp(node.returnType)
            } else {
                null
            }
        val args = List(node.args.map(emitExpr): _*)
        val call = IRCall(node.func.name, args, dest)
        append(call, node)
        call.dest
    }

    private def emitExpr(node: ASTNode): BaseSymbol = node match {
        case l: Literal             => emitLiteral(l)
        case s: ASTSymbolNode       => emitSymbol(s)
        case o: ASTOpNode           => emitOp(o)
        case a: ASTAvailableNode    => emitAvailable(a)
        case c: ASTConvertNode      => emitConvert(c)
        case c: ASTCallNode         => emitCall(c)
        case _ =>
            Error.raise("invalid expression", node)
            null
    }

    private def emitAssign(node: ASTAssignNode) {
        val src = emitExpr(node.src)
        node.dest match {
            case sn: ASTSymbolNode =>
                val dest = kt.getSymbol(sn.symbol)
                if (sn.indexes.isEmpty) {
                    append(IRInstruction(node.op, dest, src), node)
                } else {
                    val offset = emitOffset(sn)
                    append(IRStore(dest, offset, src), node)
                    kt.releaseTemp(offset)
                }
            case _ =>
                Error.raise("invalid LValue", node)
        }
        kt.releaseTemp(src)
    }

    private def emitIf(node: ASTIfNode) {

        // Emit the test.
        val test = emitExpr(node.cond)

        // Save space for the conditional.
        val condIndex = instructions.size
        instructions += null

        // Allow the test symbol to be reused.
        kt.releaseTemp(test)

        // Emit the true branch.
        val truePointer = getPointer(node)
        emit(node.iTrue, false)

        // Emit the false branch.
        if (node.iFalse != null) {

            // Save space for the goto for the true branch.
            val gotoIndex = instructions.size
            instructions += null

            // Emit the false branch.
            val falsePointer = getPointer(node)
            emit(node.iFalse, false)

            // Fill in the goto.
            val skipIndex = instructions.size
            set(gotoIndex, IRGoto(skipIndex), node)

            // Fill in the conditional.
            set(condIndex, IRConditional(test, truePointer, falsePointer), node)

        } else {
            set(condIndex, IRConditional(test, truePointer), node)
        }
    }

    private def emitSwitch(node: ASTSwitchNode) {

        // Emit the test(s).
        val test = emitExpr(node.cond)

        val hasDefault = node.cases.exists { case (t, _) => t == null }
        val cases = if (hasDefault) node.cases else node.cases :+ (null, null)
        val exprs = cases.map { case (t, _) =>
            if (t != null) emitExpr(t) else null
        }

        // Create a slot for the switch.
        val switchIndex = instructions.size
        instructions += null

        // Emit the bodies.
        val pointers = cases.map { case (_, b) =>

            // Emit the code and keep a pointer to it.
            val codePointer = instructions.size
            emit(b, false)

            // Save space for a goto, and track where it should be placed.
            val gotoPointer = instructions.size
            instructions += null

            (codePointer, gotoPointer)

        }

        val endPointer = getPointer(node)

        // Setup the gotos.
        pointers.foreach { case (_, gotoIndex) =>
            set(gotoIndex, IRGoto(endPointer), node)
        }

        // Create the switch.
        val targets = exprs.zip(pointers.map(_._1)).toSeq
        set(switchIndex, IRSwitch(test, targets), node)

    }

    private def emitWhile(node: ASTWhileNode) {

        // Emit the test.
        val top = getPointer(node)
        val test = emitExpr(node.cond)
        kt.releaseTemp(test)

        // Save space for the conditional.
        val condIndex = instructions.size
        instructions += null

        // Emit the loop body.
        val trueIndex = getPointer(node)
        emit(node.body, false)

        // Emit the goto to repeat the loop.
        append(IRGoto(top), node)

        // Setup the conditional.
        val falseIndex = getPointer(node)
        set(condIndex, IRConditional(test, trueIndex, falseIndex), node)

    }

    private def emitStop(node: ASTStopNode) {
        append(IRStop(), node)
    }

    private def emitReturn(node: ASTReturnNode) {
        val result = emitExpr(node.a)
        append(IRReturn(result), node)
    }

    private def emitBlock(node: ASTBlockNode) {
        node.children.foreach { n =>
            emit(n, false)
        }
    }

    def emit(node: ASTNode, topLevel: Boolean = true): IRGraph = {
        var mainLoop: Int = 0
        if (topLevel) {
            val values = kt.states.filter(_.value != null)
            val label = instructions.size
            val sblock = StateBlock(List(IRStart()), ast=node, label=label)
            val initial = values.foldLeft(sblock) { (b, s) =>
                val value = emitLiteral(s.value)
                val assign = IRInstruction(NodeType.assign, s, value)
                b.insert(assign)
            }
            instructions += initial
            mainLoop = getPointer(node)
        }
        node match {
            case anode: ASTAssignNode  => emitAssign(anode)
            case cond:  ASTIfNode        => emitIf(cond)
            case sw:     ASTSwitchNode  => emitSwitch(sw)
            case loop:  ASTWhileNode    => emitWhile(loop)
            case call:  ASTCallNode     => emitCall(call)
            case stop:  ASTStopNode     => emitStop(stop)
            case ret:    ASTReturnNode  => emitReturn(ret)
            case block: ASTBlockNode    => emitBlock(block)
            case null                        => ()
            case _                            =>
                Error.raise("invalid start statement", node)
        }
        if (topLevel) {

            append(IRGoto(mainLoop), node)

            // Set up next pointers.
            val withNext: Traversable[StateBlock] = instructions.map { b =>
                val next = b.label + 1
                b.jump match {
                    case cn: IRConditional =>
                        if (cn.iTrue < 0 && cn.iFalse < 0) {
                            b.replace(cn, cn.copy(iTrue = next, iFalse = next))
                        } else if (cn.iFalse < 0) {
                            b.replace(cn, cn.copy(iFalse = next))
                        } else if (cn.iTrue < 0) {
                            b.replace(cn, cn.copy(iTrue = next))
                        } else {
                            b
                        }
                    case st: IRStart if st.next < 0 =>
                        b.replace(st, st.copy(next = next))
                    case gt: IRGoto if gt.next < 0  =>
                        b.replace(gt, gt.copy(next = next))
                    case _ => b
                }
            }

            // Create the graph object.
            val graph = withNext.foldLeft(IRGraph()) { (g, sb) =>
                g.insert(sb)
            }

            return graph

        } else {

            return null

        }

    }

    override def toString =
        instructions.foldLeft("") { (a, b) => a + b.toString + "\n" }

}

