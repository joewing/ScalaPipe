
package autopipe

import autopipe.dsl.AutoPipeBlock
import scala.collection.mutable.ListBuffer

private[autopipe] class Scope(val apb: AutoPipeBlock,
                                        val nodeType: NodeType.Value,
                                        _cond: ASTNode = null) {

    var conditions = new ListBuffer[ASTNode]
    var bodies = new ListBuffer[ASTNode]

    var currentBody = new ListBuffer[ASTNode]
    var gotElse = false

    if (_cond != null) {
        conditions += _cond
    }

    def +=(n: ASTNode) {
        currentBody += n
    }

    def handleElseIf(cond: ASTNode) {
        if (gotElse) Error.raise("ELSEIF after ELSE", cond)
        if (nodeType != NodeType.IF) Error.raise("ELSEIF without IF", cond)
        conditions += cond
        currentBody -= cond
        bodies += getTop(cond)
    }

    def handleElse() {
        if (gotElse) Error.raise("multiple ELSE statements", apb)
        if (nodeType != NodeType.IF) Error.raise("ELSE without IF", apb)
        gotElse = true
        bodies += getTop()
    }

    def handleWhen(cond: ASTNode) {
        if (nodeType != NodeType.SWITCH) {
            Error.raise("'when' without 'switch'", apb)
        }
        conditions += cond
        currentBody -= cond
        bodies += getTop(cond)
    }

    private def getTop(cond: ASTNode = null): ASTNode = {
        val topList = new ListBuffer[ASTNode]
        for (e <- currentBody) {
            var node = e
            while (node.parent != null) {
                node = node.parent;
            }
            if (!(node == cond) && !topList.exists(node.eq(_))) {
                topList += node
            }
        }
        currentBody.clear
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
                    Error.raise("invalid IF statement", apb)
                    ASTStopNode(apb)
                case 1 => ASTIfNode(cl.head, bl.head, null, apb)
                case 2 => ASTIfNode(cl.head, bl.head, bl.last, apb)
                case _ => ASTIfNode(cl.head, bl.head,
                                    emit(cl.tail, bl.tail), apb)
            }
        }

        emit(conditions, bodies)

    }

    private def handleSwitchEnd(): ASTNode = {
        val node = ASTSwitchNode(conditions.head, apb)
        if (conditions.size != bodies.size) {
            Error.raise("invalid 'switch' statement", apb)
        }
        node.cases ++= conditions.tail.zip(bodies.init)
        node
    }

    private def handleWhileEnd(): ASTNode =
        ASTWhileNode(conditions.head, bodies.head, apb)

    private def handleBlockEnd(): ASTNode = bodies.head

    def handleEnd(): ASTNode = {
        bodies += getTop()
        nodeType match {
            case NodeType.IF     => handleIfEnd
            case NodeType.SWITCH => handleSwitchEnd
            case NodeType.WHILE  => handleWhileEnd
            case NodeType.BLOCK  => handleBlockEnd
            case _               =>
                Error.raise("invalid END", apb)
                ASTStopNode(apb)
        }
    }

}

