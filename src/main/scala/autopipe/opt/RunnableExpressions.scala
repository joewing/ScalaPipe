package autopipe.opt

import autopipe._

object RunnableExpressions extends DataFlowProblem {

    type T = IRNode

    def forward = false

    private def isExpression(n: IRNode): Boolean = n match {
        case in: IRInstruction  => true
        case ld: IRLoad         => true
        case st: IRStore        => true
        case _                  => false
    }

    private def hasConflictingStore(sb: StateBlock, n: IRNode): Boolean = {
        sb.nodes.exists {
            case as: IRStore => !n.dests.intersect(as.srcs).isEmpty
            case _ => false
        }
    }

    private def disjoint(a: BaseSymbol, b: BaseSymbol) = (a, b) match {
        case (ia: ImmediateSymbol, ib: ImmediateSymbol) => ia.value != ib.value
        case _ => false
    }

    private def hasConflict(a: IRNode, b: IRNode): Boolean = {
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
        graph.nodes.filter(isExpression).toSet

    def gen(sb: StateBlock, in: Set[T]): Set[T] =
        sb.nodes.filter(isExpression).toSet

    def kill(sb: StateBlock, in: Set[T]): Set[T] = {
        in.filter { i => hasConflict(i, sb) }
    }

    def meet(a: Set[T], b: Set[T]) = a.intersect(b)

}
