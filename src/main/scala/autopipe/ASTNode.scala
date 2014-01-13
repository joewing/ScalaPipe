
package autopipe

import autopipe.dsl.AutoPipeBlock
import autopipe.dsl.AutoPipeFunction
import autopipe.dsl.AutoPipeObject
import scala.collection.mutable.ListBuffer

abstract class ASTNode(
    val op: NodeType.Value,
    val apb: AutoPipeBlock) {

    private[autopipe] var parent: ASTNode = null
    private[autopipe] def children: Seq[ASTNode]
    private[autopipe] var valueType = ValueType.void
    private[autopipe] var lineNumber = 0
    private[autopipe] var fileName = ""
    private[autopipe] var isStart = false

    private[autopipe] def isPure: Boolean = children.forall(_.isPure)

    // Dirty way to get the line number and file name in case of an error.
    if (apb != null) {
        try {
            throw new Exception("DEBUG")
        } catch {
            case e: Exception =>
                val trace = e.getStackTrace().filter {
                    !_.getClassName().startsWith("autopipe.")
                }
                fileName = trace.head.getFileName
                lineNumber = trace.head.getLineNumber
        }
    }

    final def :=[T <% ASTNode](o: T) = ASTAssignNode(this, o, apb)

    final def unary_- = ASTOpNode(NodeType.neg, this, null, apb)

    final def unary_+ = this

    final def unary_! = ASTOpNode(NodeType.not, this, null, apb)

    final def unary_~ = ASTOpNode(NodeType.compl, this, null, apb)

    final def &&[T <% ASTNode](o: T) = ASTOpNode(NodeType.land, this, o, apb)

    final def ||[T <% ASTNode](o: T) = ASTOpNode(NodeType.lor, this, o, apb)

    final def &[T <% ASTNode](o: T) = ASTOpNode(NodeType.and, this, o, apb)

    final def |[T <% ASTNode](o: T) = ASTOpNode(NodeType.or, this, o, apb)

    final def ^[T <% ASTNode](o: T) = ASTOpNode(NodeType.xor, this, o, apb)

    final def >>[T <% ASTNode](o: T) = ASTOpNode(NodeType.shr, this, o, apb)

    final def <<[T <% ASTNode](o: T) = ASTOpNode(NodeType.shl, this, o, apb)

    final def +[T <% ASTNode](o: T) = ASTOpNode(NodeType.add, this, o, apb)

    final def -[T <% ASTNode](o: T) = ASTOpNode(NodeType.sub, this, o, apb)

    final def *[T <% ASTNode](o: T) = ASTOpNode(NodeType.mul, this, o, apb)

    final def /[T <% ASTNode](o: T) = ASTOpNode(NodeType.div, this, o, apb)

    final def %[T <% ASTNode](o: T) = ASTOpNode(NodeType.mod, this, o, apb)

    final def ===[T <% ASTNode](o: T) = ASTOpNode(NodeType.eq, this, o, apb)

    final def <>[T <% ASTNode](o: T) = ASTOpNode(NodeType.ne, this, o, apb)

    final def >[T <% ASTNode](o: T) = ASTOpNode(NodeType.gt, this, o, apb)

    final def <[T <% ASTNode](o: T) = ASTOpNode(NodeType.lt, this, o, apb)

    final def >=[T <% ASTNode](o: T) = ASTOpNode(NodeType.ge, this, o, apb)

    final def <=[T <% ASTNode](o: T) = ASTOpNode(NodeType.le, this, o, apb)

    final def abs = ASTOpNode(NodeType.abs, this, null, apb)

    final def exp = ASTOpNode(NodeType.exp, this, null, apb)

    final def log = ASTOpNode(NodeType.log, this, null, apb)

    final def sqrt = ASTOpNode(NodeType.sqrt, this, null, apb)

    final def sin = ASTOpNode(NodeType.sin, this, null, apb)

    final def cos = ASTOpNode(NodeType.sin, this, null, apb)

    final def tan = ASTOpNode(NodeType.sin, this, null, apb)

    private def assignOp(op: NodeType.Value, other: ASTNode) =
        ASTAssignNode(this, ASTOpNode(op, this, other, apb), apb)

    final def &&=[T <% ASTNode](o: T) = assignOp(NodeType.land, o)

    final def ||=[T <% ASTNode](o: T) = assignOp(NodeType.lor, o)

    final def &=[T <% ASTNode](o: T) = assignOp(NodeType.and, o)

    final def |=[T <% ASTNode](o: T) = assignOp(NodeType.or, o)

    final def ^=[T <% ASTNode](o: T) = assignOp(NodeType.xor, o)

    final def >>=[T <% ASTNode](o: T) = assignOp(NodeType.shr, o)

    final def <<=[T <% ASTNode](o: T) = assignOp(NodeType.shl, o)

    final def +=[T <% ASTNode](o: T) = assignOp(NodeType.add, o)

    final def -=[T <% ASTNode](o: T) = assignOp(NodeType.sub, o)

    final def *=[T <% ASTNode](o: T) = assignOp(NodeType.mul, o)

    final def /=[T <% ASTNode](o: T) = assignOp(NodeType.div, o)

    final def %=[T <% ASTNode](o: T) = assignOp(NodeType.mod, o)

}

private trait ASTStartNode extends ASTNode {
    if (apb != null) {
        apb.scopeStack.last += this
    }
    isStart = true
}

private[autopipe] case class ASTOpNode(
        _op: NodeType.Value,
        val a: ASTNode,
        val b: ASTNode = null,
        _apb: AutoPipeBlock = null)
    extends ASTNode(_op, _apb) {

    override private[autopipe] def children = List(a, b).filter { _ != null }

    a.parent = this
    if (b != null) {
        b.parent = this
    }

}

private[autopipe] case class ASTAssignNode(
        val dest: ASTNode,
        val src: ASTNode,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.assign, _apb) with ASTStartNode {

    override private[autopipe] def children = List(dest, src)

    dest.parent = this
    src.parent = this

}

private[autopipe] case class ASTIfNode(
        val cond: ASTNode,
        val iTrue: ASTNode,
        val iFalse: ASTNode,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.IF, _apb) with ASTStartNode {

    override private[autopipe] def children =
        List(cond, iTrue, iFalse).filter { _ != null }


    cond.parent = this
    if (iTrue != null) {
        iTrue.parent = this
    }
    if (iFalse != null) {
        iFalse.parent = this
    }

}

private[autopipe] case class ASTSwitchNode(
        val cond: ASTNode,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.SWITCH, _apb) with ASTStartNode {

    private[autopipe] val cases = new ListBuffer[(ASTNode, ASTNode)]

    override private[autopipe] def children =
        List(cond) ++ cases.flatMap { case (a, b) =>
            List(a, b).filter(_ != null)
        }

    cond.parent = this

}

private[autopipe] case class ASTWhileNode(
        val cond: ASTNode,
        val body: ASTNode,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.WHILE, _apb) with ASTStartNode {

    override private[autopipe] def children = List(cond, body)

    cond.parent = this
    body.parent = this

}

private[autopipe] case class ASTStopNode(_apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.STOP, _apb) with ASTStartNode {

    override private[autopipe] def children = List()

}

private[autopipe] case class ASTBlockNode(
        _nodes: Seq[ASTNode],
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.BLOCK, _apb) with ASTStartNode {

    override private[autopipe] def children = _nodes

    children.foreach { n => n.parent = this }

}

private[autopipe] case class ASTAvailableNode(
        val symbol: String,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.avail, _apb) {

    override private[autopipe] def children = List()

}

private[autopipe] case class ASTSymbolNode(
        val symbol: String,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.symbol, _apb) {

    var index: ASTNode = null

    override def children = if (index == null) List() else List(index)

    private[autopipe] override def isPure =
        if (index != null) index.isPure else valueType.isPure

    def apply(l: ASTNode): ASTNode = {
        index = l
        index.parent = this
        this
    }

    def apply(s: Symbol): ASTNode = {
        index = SymbolLiteral(s.name, apb)
        index.parent = this
        this
    }

}

private[autopipe] case class ASTCallNode(
        val func: AutoPipeFunction,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.call, _apb) with ASTStartNode {

    val symbol = func.name

    val returnType = func.returnType

    var args: Seq[ASTNode] = null

    override def children = if (args == null) List() else args

    def apply(l: ASTNode*): ASTNode = {
        args = l
        for (a <- l) {
            a.parent = this
        }
        this
    }

}

private[autopipe] case class ASTSpecial(
        val obj: AutoPipeObject,
        val method: String,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.special, _apb) with ASTStartNode {

    var args: List[ASTNode] = Nil

    override def children = args

    private[autopipe] override def isPure = false

}

private[autopipe] case class ASTConvertNode(
        val a: ASTNode,
        _valueType: ValueType,
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.convert, _apb) {

    valueType = _valueType
    if (_apb == null) {
        lineNumber = a.lineNumber
        fileName = a.fileName
    }

    override private[autopipe] def children = List(a)

    a.parent = this

}

private[autopipe] case class ASTReturnNode(
        val a: ASTNode, 
        _apb: AutoPipeBlock = null)
    extends ASTNode(NodeType.RETURN, _apb) with ASTStartNode {

    override private[autopipe] def children = List(a).filter(_ != null)

    if (a != null) {
        a.parent = this
    }

}

