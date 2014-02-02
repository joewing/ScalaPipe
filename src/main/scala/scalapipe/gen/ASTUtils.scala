package scalapipe.gen

import scalapipe._

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
        case rn: ASTAvailableNode if p(rn.symbol) => Set(rn.symbol)
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

    protected def localInputs(node: ASTNode) = localPorts(node, kt.isInput)

    protected def localOutputs(node: ASTNode) = localPorts(node, kt.isOutput)

}
