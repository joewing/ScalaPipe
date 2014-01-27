package autopipe.gen

import autopipe._

private[gen] trait ASTUtils {

    val kt: KernelType

    protected def isNative(vt: ValueType) = vt.isInstanceOf[NativeValueType]

    protected def isNativePointer(vt: ValueType) = vt match {
        case p: PointerValueType if isNative(p.itemType) => true
        case _ => false
    }

    private def localPorts(node: ASTNode,
                           p: String => Boolean): Set[String] = node match {
        case sn: ASTSymbolNode =>
            val indexPorts = sn.indexes.flatMap(localPorts(_, p)).toSet
            if (p(sn.symbol)) {
                indexPorts + sn.symbol
            } else {
                indexPorts
            }
        case cn: ASTCallNode    => cn.args.flatMap(localPorts(_, p)).toSet
        case on: ASTOpNode      => localPorts(on.a, p) ++ localPorts(on.b, p)
        case cn: ASTConvertNode => localPorts(cn.a, p)
        case an: ASTAssignNode  =>
            localPorts(an.dest, p) ++ localPorts(an.src, p)
        case in: ASTIfNode      => localPorts(in.cond, p)
        case wn: ASTWhileNode   => localPorts(wn.cond, p)
        case sn: ASTSwitchNode  => localPorts(sn.cond, p)
        case bn: ASTBlockNode   => bn.children.flatMap(localPorts(_, p)).toSet
        case rn: ASTAvailableNode if kt.isInput(rn.symbol) => Set(rn.symbol)
        case rt: ASTReturnNode =>
            if (!kt.outputs.isEmpty && p(kt.outputs.head.name)) {
                localPorts(rt.a, p) + kt.outputs.head.name
            } else {
                localPorts(rt.a, p)
            }
        case _ => Set()
    }

    private def blockingPorts(node: ASTNode,
                              p: String => Boolean): Set[String] = node match {

        case sn: ASTSymbolNode =>
            val indexPorts = sn.indexes.flatMap(blockingPorts(_, p)).toSet
            if (p(sn.symbol)) {
                indexPorts + sn.symbol
            } else {
                indexPorts
            }
        case cn: ASTCallNode =>
            cn.args.flatMap(blockingPorts(_, p)).toSet
        case on: ASTOpNode =>
            blockingPorts(on.a, p) ++ blockingPorts(on.b, p)
        case cn: ASTConvertNode => blockingPorts(cn.a, p)
        case an: ASTAssignNode =>
            blockingPorts(an.dest, p) ++ blockingPorts(an.src, p)
        case in: ASTIfNode => blockingPorts(in.cond, p)
        case wn: ASTWhileNode => blockingPorts(wn.cond, p)
        case sn: ASTSwitchNode => blockingPorts(sn.cond, p)
        case rt: ASTReturnNode =>
            if (!kt.outputs.isEmpty && p(kt.outputs.head.name)) {
                blockingPorts(rt.a, p) + kt.outputs.head.name
            } else {
                blockingPorts(rt.a, p)
            }
        case _ => Set()

    }

    protected def blockingInputs(node: ASTNode) =
        blockingPorts(node, kt.isInput)

    protected def localOutputs(node: ASTNode) =
        localPorts(node, kt.isOutput)

    protected def reads(node: ASTNode): Set[(String, ASTNode)] = {
        node match {
            case an: ASTAssignNode  => reads(an.src)
            case on: ASTOpNode      => reads(on.a) ++ reads(on.b)
            case sn: ASTSymbolNode  =>
                sn.indexes.flatMap(i => reads(i) + ((sn.symbol, i))).toSet
            case _                  => Set()
        }
    }

    protected def writes(node: ASTNode,
                         assign: Boolean = false): Set[(String, ASTNode)] = {
        if (assign) {
            node match {
                case sn: ASTSymbolNode =>
                    sn.indexes.map(i => ((sn.symbol, i))).toSet
                case _ => Set()
            }
        } else {
            node match {
                case an: ASTAssignNode => writes(an.dest, true)
                case _ => Set()
            }
        }
    }

    protected def requiresInput(node: ASTNode): Boolean = node match {
        case sn: ASTSymbolNode =>
            kt.isInput(sn.symbol) || sn.indexes.exists(requiresInput(_))
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
        case rn: ASTAvailableNode if kt.isInput(rn.symbol) => true
        case rt: ASTReturnNode => requiresInput(rt.a)
        case _ => false
    }

}
