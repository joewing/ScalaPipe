package autopipe

import autopipe.dsl.{Kernel, Func, AutoPipeObject}

abstract class ASTNode(
        val op: NodeType.Value,
        val kernel: Kernel
    ) extends DebugInfo {

    private[autopipe] var parent: ASTNode = null
    private[autopipe] def children: Seq[ASTNode]
    private[autopipe] var valueType = ValueType.void
    private[autopipe] var isStart = false

    private[autopipe] def pure: Boolean = children.forall(_.pure)

    // Dirty way to get the line number and file name in case of an error.
    if (kernel != null) {
        collectDebugInfo
    }

    final def unary_- = ASTOpNode(NodeType.neg, this, null, kernel)

    final def unary_+ = this

    final def unary_! = ASTOpNode(NodeType.not, this, null, kernel)

    final def unary_~ = ASTOpNode(NodeType.compl, this, null, kernel)

    final def &&[T <% ASTNode](o: T) = ASTOpNode(NodeType.land, this, o, kernel)

    final def ||[T <% ASTNode](o: T) = ASTOpNode(NodeType.lor, this, o, kernel)

    final def &[T <% ASTNode](o: T) = ASTOpNode(NodeType.and, this, o, kernel)

    final def |[T <% ASTNode](o: T) = ASTOpNode(NodeType.or, this, o, kernel)

    final def ^[T <% ASTNode](o: T) = ASTOpNode(NodeType.xor, this, o, kernel)

    final def >>[T <% ASTNode](o: T) = ASTOpNode(NodeType.shr, this, o, kernel)

    final def <<[T <% ASTNode](o: T) = ASTOpNode(NodeType.shl, this, o, kernel)

    final def +[T <% ASTNode](o: T) = ASTOpNode(NodeType.add, this, o, kernel)

    final def -[T <% ASTNode](o: T) = ASTOpNode(NodeType.sub, this, o, kernel)

    final def *[T <% ASTNode](o: T) = ASTOpNode(NodeType.mul, this, o, kernel)

    final def /[T <% ASTNode](o: T) = ASTOpNode(NodeType.div, this, o, kernel)

    final def %[T <% ASTNode](o: T) = ASTOpNode(NodeType.mod, this, o, kernel)

    final def ===[T <% ASTNode](o: T) = ASTOpNode(NodeType.eq, this, o, kernel)

    final def <>[T <% ASTNode](o: T) = ASTOpNode(NodeType.ne, this, o, kernel)

    final def >[T <% ASTNode](o: T) = ASTOpNode(NodeType.gt, this, o, kernel)

    final def <[T <% ASTNode](o: T) = ASTOpNode(NodeType.lt, this, o, kernel)

    final def >=[T <% ASTNode](o: T) = ASTOpNode(NodeType.ge, this, o, kernel)

    final def <=[T <% ASTNode](o: T) = ASTOpNode(NodeType.le, this, o, kernel)

    final def abs = ASTOpNode(NodeType.abs, this, null, kernel)

    final def exp = ASTOpNode(NodeType.exp, this, null, kernel)

    final def log = ASTOpNode(NodeType.log, this, null, kernel)

    final def sqrt = ASTOpNode(NodeType.sqrt, this, null, kernel)

    final def sin = ASTOpNode(NodeType.sin, this, null, kernel)

    final def cos = ASTOpNode(NodeType.sin, this, null, kernel)

    final def tan = ASTOpNode(NodeType.sin, this, null, kernel)

    private def assignOp(op: NodeType.Value, other: ASTNode) =
        ASTAssignNode(this, ASTOpNode(op, this, other, kernel), kernel)

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
    if (kernel != null) {
        kernel.scopeStack.last += this
    }
    isStart = true
}

private[autopipe] case class ASTOpNode(
        _op: NodeType.Value,
        val a: ASTNode,
        val b: ASTNode = null,
        _kernel: Kernel = null
    ) extends ASTNode(_op, _kernel) {

    override private[autopipe] def children = Seq(a, b).filter(_ != null)

    a.parent = this
    if (b != null) {
        b.parent = this
    }

}

private[autopipe] case class ASTAssignNode(
        val dest: ASTNode,
        val src: ASTNode,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.assign, _kernel) with ASTStartNode {

    override private[autopipe] def children = Seq(dest, src)

    dest.parent = this
    src.parent = this

}

private[autopipe] case class ASTIfNode(
        val cond: ASTNode,
        val iTrue: ASTNode,
        val iFalse: ASTNode,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.IF, _kernel) with ASTStartNode {

    override private[autopipe] def children =
        Seq(cond, iTrue, iFalse).filter(_ != null)


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
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.SWITCH, _kernel) with ASTStartNode {

    private[autopipe] var cases = Seq[(ASTNode, ASTNode)]()

    override private[autopipe] def children =
        Seq(cond) ++ cases.flatMap { case (a, b) =>
            Seq(a, b).filter(_ != null)
        }

    cond.parent = this

}

private[autopipe] case class ASTWhileNode(
        val cond: ASTNode,
        val body: ASTNode,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.WHILE, _kernel) with ASTStartNode {

    override private[autopipe] def children = Seq(cond, body)

    cond.parent = this
    body.parent = this

}

private[autopipe] case class ASTStopNode(
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.STOP, _kernel) with ASTStartNode {

    override private[autopipe] def children = Seq()

}

private[autopipe] case class ASTBlockNode(
        nodes: Seq[ASTNode],
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.BLOCK, _kernel) with ASTStartNode {

    override private[autopipe] def children = nodes

    children.foreach { n => n.parent = this }

}

private[autopipe] case class ASTAvailableNode(
        val symbol: String,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.avail, _kernel) {

    override private[autopipe] def children = Seq()

}

private[autopipe] case class ASTSymbolNode(
        val symbol: String,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.symbol, _kernel) {

    private[autopipe] var indexes = Seq[ASTNode]()

    override def children = indexes

    private[autopipe] override def pure =
        valueType.pure && indexes.forall(_.pure)

    def apply(l: ASTNode): ASTSymbolNode = {
        indexes = indexes :+ l
        l.parent = this
        this
    }

    def apply(s: Symbol): ASTSymbolNode = {
        val index = SymbolLiteral(s.name, kernel)
        index.parent = this
        indexes = indexes :+ index
        this
    }

    def update(l: ASTNode, r: ASTNode): ASTAssignNode = {
        indexes = indexes :+ l
        l.parent = this
        ASTAssignNode(this, r, kernel)
    }

    def update(s: Symbol, r: ASTNode): ASTAssignNode = {
        val index = SymbolLiteral(s.name, kernel)
        index.parent = this
        indexes = indexes :+ index
        ASTAssignNode(this, r, kernel)
    }

    override def toString = symbol + "(" + indexes.mkString(",") + ")"

}

private[autopipe] case class ASTCallNode(
        val func: Func,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.call, _kernel) with ASTStartNode {

    val symbol = func.name

    val returnType = func.outputs.headOption match {
        case Some(o)    => o.valueType
        case None       => ValueType.void
    }

    var args = Seq[ASTNode]()

    override def children = args

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
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.special, _kernel) with ASTStartNode {

    var args = Seq[ASTNode]()

    override def children = args

    private[autopipe] override def pure = false

}

private[autopipe] case class ASTConvertNode(
        val a: ASTNode,
        _valueType: ValueType,
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.convert, _kernel) {

    valueType = _valueType
    if (kernel == null) {
        lineNumber = a.lineNumber
        fileName = a.fileName
    }

    override private[autopipe] def children = Seq(a)

    a.parent = this

}

private[autopipe] case class ASTReturnNode(
        val a: ASTNode, 
        _kernel: Kernel = null
    ) extends ASTNode(NodeType.RETURN, _kernel) with ASTStartNode {

    override private[autopipe] def children = Seq(a).filter(_ != null)

    if (a != null) {
        a.parent = this
    }

}
