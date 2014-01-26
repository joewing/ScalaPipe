package autopipe.opt

import autopipe._

object RunnableExpressions extends DataFlowProblem {

    type T = IRNode

    def forward = false

    private def isExpression(n: IRNode): Boolean = n match {
        case in: IRInstruction  => true
        case al: IRArrayLoad    => true
        case as: IRArrayStore   => true
        case vl: IRVectorLoad   => true
        case vs: IRVectorStore  => true
        case _                  => false
    }

    private def hasConflictingStore(sb: StateBlock, n: IRNode): Boolean = {
        sb.nodes.exists {
            case as: IRArrayStore => !n.dests.intersect(as.srcs).isEmpty
            case _ => false
        }
    }

    private def disjoint(a: BaseSymbol, b: BaseSymbol) = (a, b) match {
        case (ia: ImmediateSymbol, ib: ImmediateSymbol) => ia.value != ib.value
        case _ => false
    }

    private def hasConflict(a: IRNode, b: IRNode): Boolean = (a, b) match {
        case (va: IRVectorLoad, vb: IRVectorLoad) =>
            // Note that we don't need to check src since it is the vector.
            va.offset == vb.dest || vb.offset == va.dest || va.dest == vb.dest
        case (vs: IRVectorStore, vl: IRVectorLoad) =>
            (vs.dest == vl.src && !disjoint(vs.offset, vl.offset)) ||
            vs.src == vl.dest || vs.offset == vl.dest
        case (vl: IRVectorLoad, vs: IRVectorStore) =>
            (vs.dest == vl.src && !disjoint(vs.offset, vl.offset)) ||
            vs.src == vl.dest || vs.offset == vl.dest
        case (va: IRVectorStore, vb: IRVectorStore) =>
            // Only modifying the vector.
            (va.dest == vb.dest && !disjoint(va.offset, vb.offset))
        case _ =>
            !a.dests.intersect(b.dests).isEmpty ||
            !b.srcs.intersect(a.dests).isEmpty ||
            !a.srcs.intersect(b.dests).isEmpty ||
            (a.dests.exists(_.isInstanceOf[OutputSymbol]) &&
             b.srcs.exists(_.isInstanceOf[InputSymbol])) ||
            (b.dests.exists(_.isInstanceOf[OutputSymbol]) &&
             a.srcs.exists(_.isInstanceOf[InputSymbol]))
    }

    private def hasConflict(a: IRNode, b: StateBlock): Boolean = {
        b.nodes.exists(n => hasConflict(a, n))
    }

    def init(kt: KernelType, graph: IRGraph) =
        Set[T](graph.nodes.filter(isExpression): _*)

    def gen(sb: StateBlock, in: Set[T]): Set[T] =
        Set[T](sb.nodes.filter(isExpression): _*)

    def kill(sb: StateBlock, in: Set[T]): Set[T] = {
        in.filter { i => hasConflict(i, sb) }
    }

    def meet(a: Set[T], b: Set[T]) = a.intersect(b)

}
