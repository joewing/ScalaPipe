package scalapipe.gen

import scalapipe._

private[gen] trait ASTUtils {

    val kt: KernelType

    protected def isNative(vt: ValueType) = vt.isInstanceOf[NativeValueType]

    protected def isNativePointer(vt: ValueType) = vt match {
        case p: PointerValueType if isNative(p.itemType) => true
        case _ => false
    }

    /** Return destination symbols for a node. */
    protected def localDests(node: ASTNode): Seq[ASTSymbolNode] = node match {
        case an: ASTAssignNode      => Seq(an.dest)
        case an: ASTAvailableNode   => Seq()
        case bn: ASTBlockNode       => Seq()
        case _ => node.children.flatMap(localDests)
    }

    /** Return source symbols for a node. */
    protected def localSources(node: ASTNode): Seq[ASTSymbolNode] = node match {
        case sn: ASTSymbolNode      => Seq(sn)
        case an: ASTAssignNode      => localSources(an.src)
        case an: ASTAvailableNode   => Seq()
        case bn: ASTBlockNode       => Seq()
        case _ => node.children.flatMap(localSources)
    }

    /** Return all symbols names for a node. */
    protected def localSymbols(node: ASTNode): Seq[String] = node match {
        case sn: ASTSymbolNode  => Seq(sn.symbol)
        case bn: ASTBlockNode   => Seq()
        case _ => node.children.flatMap(localSymbols)
    }

    protected def localInputs(node: ASTNode): Seq[String]  =
        localSymbols(node).filter(kt.isInput)

    protected def localOutputs(node: ASTNode): Seq[String] =
        localSymbols(node).filter(kt.isOutput)

}
