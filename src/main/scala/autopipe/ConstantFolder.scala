
package autopipe

import scala.language.implicitConversions
import scala.collection.mutable.ListBuffer

private[autopipe] object ConstantFolder {

    def fold(co: CodeObject, root: ASTNode): ASTNode = {
        val folder = new ConstantFolder(co)
        folder.fold(root)
    }

}

private[autopipe] class ConstantFolder(co: CodeObject) {

    implicit private def bool(v: Long): Boolean = v != 0

    implicit private def bool(v: Double): Boolean = v != 0.0

    private def foldIntOp(node: ASTOpNode,
                          a: IntLiteral,
                          b: IntLiteral): ASTNode = {
        val apb = node.apb
        node.op match {
            case NodeType.addr  => ASTOpNode(node.op, a, null, apb)
            case NodeType.neg   => IntLiteral(-a.value, apb)
            case NodeType.compl => IntLiteral(~a.value, apb)
            case NodeType.and   => IntLiteral(a.value & b.value, apb)
            case NodeType.or    => IntLiteral(a.value | b.value, apb)
            case NodeType.xor   => IntLiteral(a.value ^ b.value, apb)
            case NodeType.shr   => IntLiteral(a.value >> b.value, apb)
            case NodeType.shl   => IntLiteral(a.value << b.value, apb)
            case NodeType.add   => IntLiteral(a.value + b.value, apb)
            case NodeType.sub   => IntLiteral(a.value - b.value, apb)
            case NodeType.mul   => IntLiteral(a.value * b.value, apb)
            case NodeType.div   => IntLiteral(a.value / b.value, apb)
            case NodeType.mod   => IntLiteral(a.value % b.value, apb)
            case NodeType.not   => IntLiteral(a.value == 0, apb)
            case NodeType.land  => IntLiteral(a.value && b.value, apb)
            case NodeType.lor   => IntLiteral(a.value || b.value, apb)
            case NodeType.eq    => IntLiteral(a.value == b.value, apb)
            case NodeType.ne    => IntLiteral(a.value != b.value, apb)
            case NodeType.gt    => IntLiteral(a.value > b.value, apb)
            case NodeType.lt    => IntLiteral(a.value < b.value, apb)
            case NodeType.ge    => IntLiteral(a.value >= b.value, apb)
            case NodeType.le    => IntLiteral(a.value <= b.value, apb)
            case NodeType.abs   => IntLiteral(math.abs(a.value), apb)
            case NodeType.avail => ASTOpNode(node.op, a, null, apb)
            case _              => sys.error("internal")
        }
    }

    private def foldFloatOp(node: ASTOpNode,
                            a: FloatLiteral,
                            b: FloatLiteral): ASTNode = {
        val apb = node.apb
        node.op match {
            case NodeType.addr  => ASTOpNode(node.op, a, null, apb)
            case NodeType.neg   => FloatLiteral(-a.value, apb)
            case NodeType.add   => FloatLiteral(a.value + b.value, apb)
            case NodeType.sub   => FloatLiteral(a.value - b.value, apb)
            case NodeType.mul   => FloatLiteral(a.value * b.value, apb)
            case NodeType.div   => FloatLiteral(a.value / b.value, apb)
            case NodeType.mod   => FloatLiteral(a.value % b.value, apb)
            case NodeType.not   => IntLiteral(!bool(a.value), apb)
            case NodeType.land  => IntLiteral(a.value && b.value, apb)
            case NodeType.lor   => IntLiteral(a.value || b.value, apb)
            case NodeType.eq    => IntLiteral(a.value == b.value, apb)
            case NodeType.ne    => IntLiteral(a.value != b.value, apb)
            case NodeType.gt    => IntLiteral(a.value > b.value, apb)
            case NodeType.lt    => IntLiteral(a.value < b.value, apb)
            case NodeType.ge    => IntLiteral(a.value >= b.value, apb)
            case NodeType.le    => IntLiteral(a.value <= b.value, apb)
            case NodeType.abs   => FloatLiteral(math.abs(a.value), apb)
            case NodeType.exp   => FloatLiteral(math.exp(a.value), apb)
            case NodeType.log   => FloatLiteral(math.log(a.value), apb)
            case NodeType.sqrt  => FloatLiteral(math.sqrt(a.value), apb)
            case NodeType.sin   => FloatLiteral(math.sin(a.value), apb)
            case NodeType.cos   => FloatLiteral(math.cos(a.value), apb)
            case NodeType.tan   => FloatLiteral(math.tan(a.value), apb)
            case NodeType.avail => ASTOpNode(node.op, a, null, apb)
            case _              => Error.raise("internal", apb)
        }
    }

    private def foldOp(node: ASTOpNode): ASTNode = {
        val a = fold(node.a)
        val b = fold(node.b)
        val newNode = (a, b) match {
            case (ai: IntLiteral, bi: IntLiteral) =>
                foldIntOp(node, ai, bi)
            case (af: FloatLiteral, bf: FloatLiteral) =>
                foldFloatOp(node, af, bf)
            case _ => ASTOpNode(node.op, a, b)
        }
        newNode.valueType = node.valueType
        newNode
    }

    private def foldAssign(node: ASTAssignNode): ASTNode =
        ASTAssignNode(fold(node.dest), fold(node.src))

    private def foldIf(node: ASTIfNode): ASTNode =
        ASTIfNode(fold(node.cond), fold(node.iTrue), fold(node.iFalse))

    private def foldSwitch(node: ASTSwitchNode): ASTNode = {
        val newNode = ASTSwitchNode(fold(node.cond))
        newNode.cases ++= node.cases.map { old =>
            (fold(old._1), fold(old._2))
        }
        newNode
    }

    private def foldWhile(node: ASTWhileNode): ASTNode =
        ASTWhileNode(fold(node.cond), fold(node.body))

    private def foldBlock(node: ASTBlockNode): ASTNode = {
        val children = new ListBuffer[ASTNode]
        node.children.foreach { children += fold(_) }
        ASTBlockNode(children)
    }

    private def foldCall(node: ASTCallNode): ASTNode = {
        val result = new ASTCallNode(node.func)
        val children = new ListBuffer[ASTNode]
        node.children.foreach { children += fold(_) }
        result.apply(children: _*)
        result.valueType = node.valueType
        result
    }

    private def foldConvert(node: ASTConvertNode): ASTNode = {
        val sub = fold(node.a)
        sub match {
            case l: Literal => TypeConverter.convert(l, node.valueType)
            case _ => ASTConvertNode(sub, node.valueType)
        }
    }

    private def foldSymbol(node: ASTSymbolNode): ASTNode = {
        val result = ASTSymbolNode(node.symbol)
        if (node.index != null) {
            result.index = fold(node.index)
        }
        result.valueType = node.valueType
        result
    }

    private def foldReturn(node: ASTReturnNode): ASTNode = {
        ASTReturnNode(fold(node.a))
    }

    def fold(node: ASTNode): ASTNode = {
        val newNode = node match {
            case an: ASTAssignNode  => foldAssign(an)
            case in: ASTIfNode      => foldIf(in)
            case sw: ASTSwitchNode  => foldSwitch(sw)
            case wn: ASTWhileNode   => foldWhile(wn)
            case bn: ASTBlockNode   => foldBlock(bn)
            case cn: ASTCallNode    => foldCall(cn)
            case on: ASTOpNode      => foldOp(on)
            case cn: ASTConvertNode => foldConvert(cn)
            case sn: ASTSymbolNode  => foldSymbol(sn)
            case rn: ASTReturnNode  => foldReturn(rn)
            case null               => null
            case _                  => node
        }
        if (newNode != null) {
            newNode.fileName = node.fileName
            newNode.lineNumber = node.lineNumber
        }
        newNode
    }

}

