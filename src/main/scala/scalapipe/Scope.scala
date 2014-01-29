package scalapipe

import scalapipe.dsl.Kernel

private[scalapipe] class Scope(
        val kernel: Kernel,
        val nodeType: NodeType.Value,
        _cond: ASTNode = null
    ) {

    private[scalapipe] var conditions = Seq(_cond).filter(_ != null)
    private[scalapipe] var bodies = Seq[ASTNode]()
    private var currentBody = Seq[ASTNode]()
    private var gotElse = false

    def +=(n: ASTNode) {
        currentBody = currentBody :+ n
    }

    def handleElse() {
        if (gotElse) Error.raise("multiple ELSE statements", kernel)
        if (nodeType != NodeType.IF) Error.raise("ELSE without IF", kernel)
        gotElse = true
        bodies = bodies :+ getTop()
    }

    def handleWhen(cond: ASTNode) {
        if (nodeType != NodeType.SWITCH) {
            Error.raise("'when' without 'switch'", kernel)
        }
        conditions = conditions :+ cond
        currentBody = currentBody.filterNot(_ == cond)
        bodies = bodies :+ getTop(cond)
    }

    private def getTop(cond: ASTNode = null): ASTNode = {
        var topList = Seq[ASTNode]()
        for (e <- currentBody) {
            var node = e
            while (node.parent != null) {
                node = node.parent;
            }
            if (!(node == cond) && !topList.exists(node.eq(_))) {
                topList = topList :+ node
            }
        }
        currentBody = Seq()
        val top = topList.filter { _.parent == null }
        top.size match {
            case 1 => top.head
            case _ => ASTBlockNode(top)
        }
        ASTBlockNode(top)
    }

    private def handleIfEnd(): ASTNode = {

        def emit(cl: Seq[ASTNode], bl: Seq[ASTNode]): ASTNode = {
            bl.size match {
                case 0 =>
                    Error.raise("invalid IF statement", kernel)
                    ASTStopNode(kernel)
                case 1 => ASTIfNode(cl.head, bl.head, null, kernel)
                case 2 => ASTIfNode(cl.head, bl.head, bl.last, kernel)
                case _ => ASTIfNode(cl.head, bl.head,
                                    emit(cl.tail, bl.tail), kernel)
            }
        }

        emit(conditions, bodies)

    }

    private def handleSwitchEnd(): ASTNode = {
        val node = ASTSwitchNode(conditions.head, kernel)
        if (conditions.size != bodies.size) {
            Error.raise("invalid 'switch' statement", kernel)
        }
        node.cases ++= conditions.tail.zip(bodies.init)
        node
    }

    private def handleWhileEnd(): ASTNode =
        ASTWhileNode(conditions.head, bodies.head, kernel)

    private def handleBlockEnd(): ASTNode = bodies.head

    def handleEnd(): ASTNode = {
        bodies = bodies :+ getTop()
        nodeType match {
            case NodeType.IF     => handleIfEnd
            case NodeType.SWITCH => handleSwitchEnd
            case NodeType.WHILE  => handleWhileEnd
            case NodeType.BLOCK  => handleBlockEnd
            case _               =>
                Error.raise("invalid END", kernel)
                ASTStopNode(kernel)
        }
    }

}
