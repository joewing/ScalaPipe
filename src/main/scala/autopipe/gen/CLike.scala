
package autopipe.gen

import scala.collection.immutable.ListSet
import autopipe._

private[gen] trait CLike {

    val bt: BlockType

    protected def getLocalPorts(node: ASTNode,
        p: String => Boolean): ListSet[String] = node match {

        case sn: ASTSymbolNode =>
            if (p(sn.symbol)) {
                ListSet(sn.symbol) ++ getLocalPorts(sn.index, p)
            } else {
                getLocalPorts(sn.index, p)
            }
        case cn: ASTCallNode =>
            cn.args.foldLeft(ListSet(): ListSet[String]) { (a, b) =>
                a ++ getLocalPorts(b, p)
            }
        case on: ASTOpNode =>
            getLocalPorts(on.a, p) ++ getLocalPorts(on.b, p)
        case cn: ASTConvertNode => getLocalPorts(cn.a, p)
        case an: ASTAssignNode =>
            getLocalPorts(an.dest, p) ++ getLocalPorts(an.src, p)
        case in: ASTIfNode => getLocalPorts(in.cond, p)
        case wn: ASTWhileNode => getLocalPorts(wn.cond, p)
        case sn: ASTSwitchNode => getLocalPorts(sn.cond, p)
        case bn: ASTBlockNode =>
            bn.children.foldLeft(ListSet(): ListSet[String]) { (a, b) =>
                a ++ getLocalPorts(b, p)
            }
        case rn: ASTAvailableNode if bt.isInput(rn.symbol) => ListSet(rn.symbol)
        case rt: ASTReturnNode =>
            if (p("output"))
                ListSet("output") ++ getLocalPorts(rt.a, p)
            else
                getLocalPorts(rt.a, p)
        case _ => ListSet()

    }

    protected def getBlockingPorts(node: ASTNode,
        p: String => Boolean): ListSet[String] = node match {

        case sn: ASTSymbolNode =>
            if (p(sn.symbol)) {
                ListSet(sn.symbol) ++ getBlockingPorts(sn.index, p)
            } else {
                getBlockingPorts(sn.index, p)
            }
        case cn: ASTCallNode =>
            cn.args.foldLeft(ListSet(): ListSet[String]) { (a, b) =>
                a ++ getBlockingPorts(b, p)
            }
        case on: ASTOpNode =>
            getBlockingPorts(on.a, p) ++ getBlockingPorts(on.b, p)
        case cn: ASTConvertNode => getBlockingPorts(cn.a, p)
        case an: ASTAssignNode =>
            getBlockingPorts(an.dest, p) ++ getBlockingPorts(an.src, p)
        case in: ASTIfNode => getBlockingPorts(in.cond, p)
        case wn: ASTWhileNode => getBlockingPorts(wn.cond, p)
        case sn: ASTSwitchNode => getBlockingPorts(sn.cond, p)
        case rt: ASTReturnNode =>
            if (p("output"))
                ListSet("output") ++ getBlockingPorts(rt.a, p)
            else
                getBlockingPorts(rt.a, p)
        case _ => ListSet()

    }

    protected def getBlockingInputs(node: ASTNode) =
        getBlockingPorts(node, bt.isInput)

    protected def getLocalOutputs(node: ASTNode) =
        getLocalPorts(node, bt.isOutput)

    protected def getReads(node: ASTNode): ListSet[(String, ASTNode)] = {
        node match {
            case an: ASTAssignNode  => getReads(an.src)
            case on: ASTOpNode        => getReads(on.a) ++ getReads(on.b)
            case sn: ASTSymbolNode  =>
                ListSet((sn.symbol, sn.index)) ++ getReads(sn.index)
            case _                        => ListSet()
        }
    }

    protected def getWrites(
            node: ASTNode,
            assign: Boolean = false): ListSet[(String, ASTNode)] = {
        if (assign) {
            node match {
                case sn: ASTSymbolNode  => ListSet((sn.symbol, sn.index))
                case _                        => ListSet()
            }
        } else {
            node match {
                case an: ASTAssignNode  => getWrites(an.dest, true)
                case _                        => ListSet()
            }
        }
    }

    protected def requiresInput(node: ASTNode): Boolean = node match {
        case sn: ASTSymbolNode =>
            bt.isInput(sn.symbol) || requiresInput(sn.index)
        case cn: ASTCallNode =>
            cn.args.foldLeft(false) { (a, b) => a || requiresInput(b) }
        case on: ASTOpNode =>
            requiresInput(on.a) || requiresInput(on.b)
        case cn: ASTConvertNode => requiresInput(cn.a)
        case an: ASTAssignNode =>
            requiresInput(an.dest) || requiresInput(an.src)
        case in: ASTIfNode => requiresInput(in.cond)
        case wn: ASTWhileNode => requiresInput(wn.cond)
        case sn: ASTSwitchNode => requiresInput(sn.cond)
        case bn: ASTBlockNode => 
            bn.children.foldLeft(false) { (a, b) => a || requiresInput(b) }
        case rn: ASTAvailableNode if bt.isInput(rn.symbol) => true
        case rt: ASTReturnNode => requiresInput(rt.a)
        case _ => false
    }

}

