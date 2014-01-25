
package autopipe

import scala.collection.immutable.ListSet

private[autopipe] object LocalExtractor {

    def extract(kt: KernelType) {
        val extractor = new LocalExtractor(kt)
        extractor.extract
    }

}

private[autopipe] class LocalExtractor(val k: KernelType) {

    def extract {

/*
        // Get a list of all entry points to the block.
        // An entry point is the top of the block, and before every input
        // and every output.
        val root = kt.impl.expression
        val entries = getEntries(root, ListSet(root))

        // Walk the AST.
        // For each state variable, if a use before a definition is
        // found, the variable is assumed to be a state variable.  Otherwise,
        // it can be converted to a local variable.
        for (s <- kt.states) {

            // Check if this value is used before being defined at any
            // entry point.
            if (!entries.exists { e => check(s, e)._2 }) {

                // Mark this state variable as local.
// FIXME
//                s.isLocal = true

            }
        }
*/

    }

    // Determine if a port is referenced.
/*
    private def hasPort(node: ASTNode): Boolean = {
        node.children.foldLeft(false) { (a, c) =>
            a || (c match {
                case sn: ASTSymbolNode if kt.isPort(sn.symbol) => true
                case _ => hasPort(c)
            })
        }
    }

    // Get the start expression for a node.
    private def getStart(node: ASTNode): ASTNode =
        if (node.isStart) {
            node
        } else {
            getStart(node.parent)
        }

    // Get a list of entry points.
    private def getEntries(node: ASTNode,
                                  lst: ListSet[ASTNode]): ListSet[ASTNode] = {
        node.children.foldLeft(lst) { (l, c) =>
            c match {
                case sn: ASTSymbolNode if kt.isPort(sn.symbol) =>
                    getEntries(c, l + getStart(c))
                case wn: ASTWhileNode if hasPort(wn) =>
                    getEntries(c, l + c)
                case _ => getEntries(c, l)
            }
        }
    }

    // Returns true if sym is contained in node.
    private def contains(sym: BaseSymbol, node: ASTNode): Boolean =
        node match {
        case sn: ASTSymbolNode =>
            if (sn.symbol == sym.name) {
                true
            } else {
                contains(sym, sn.index)
            }
        case null    => false
        case _        => node.children.exists { c => contains(sym, c) }
    }

    // Returns (known, used)
    private def checkAssign(sym: BaseSymbol, node: ASTAssignNode) = {
        if (contains(sym, node.src)) {
            (true, true)
        } else if (contains(sym, node.dest)) {
            (true, false)
        } else {
            (false, false)
        }
    }

    // Returns (known, used)
    private def checkIf(sym: BaseSymbol, node: ASTIfNode) = {
        if (contains(sym, node.cond)) {
            (true, true)
        } else {
            val a = check(sym, node.iTrue)
            val b = check(sym, node.iFalse)
            if ((a._1 && a._2) || (b._1 && b._2)) {
                // Used along one of the paths.
                (true, true)
            } else if (a._1 && !a._2 && b._1 && !b._2) {
                // Assigned along both paths.
                (true, false)
            } else {
                // Everything else.
                (false, false)
            }
        }
    }

    // Returns (known, used)
    private def checkWhile(sym: BaseSymbol, node: ASTWhileNode) = {
        if (contains(sym, node.cond)) {
            (true, true)
        } else {
            check(sym, node.body)
        }
    }

    // Returns (known, used)
    private def checkBlock(sym: BaseSymbol, node: ASTBlockNode) = {
        node.children.foldLeft((false, false)) { (a, n) =>
            if (a._1) {
                a
            } else {
                check(sym, n)
            }
        }
    }

    // Returns (known, used)
    private def checkCall(sym: BaseSymbol, node: ASTCallNode) = {
        if (node.children.exists { c => contains(sym, c) }) {
            (true, true)
        } else {
            (false, false)
        }
    }

    // Determine if a symbol is used without being assigned.
    // Returns (known, used).
    private def check(sym: BaseSymbol, node: ASTNode): (Boolean, Boolean) =
        node match {
            case an: ASTAssignNode      => checkAssign(sym, an)
            case in: ASTIfNode            => checkIf(sym, in)
            case wn: ASTWhileNode        => checkWhile(sym, wn)
            case sn: ASTStopNode         => (false, false)
            case bn: ASTBlockNode        => checkBlock(sym, bn)
            case cn: ASTCallNode         => checkCall(sym, cn)
            case null                        => (false, false)
            case _                            => (false, false)
        }
*/

}

