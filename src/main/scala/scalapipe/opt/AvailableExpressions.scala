package scalapipe.opt

import scalapipe._

object AvailableExpressions extends DataFlowProblem {

    type T = IRNode

    def forward = true

    private def isExpression(n: IRNode): Boolean =
        !n.dests.isEmpty && !n.symbols.exists(isPort)

    def init(kt: KernelType, graph: IRGraph) = Set[T]()

    def gen(sb: StateBlock, in: Set[T]): Set[T] = {
        val nodes = sb.nodes.filter { n =>
            isExpression(n) && n.srcs.intersect(sb.dests).isEmpty
        }
        nodes.toSet
    }

    def kill(sb: StateBlock, in: Set[T]): Set[T] =
        in.filter(p => !p.srcs.intersect(sb.dests).isEmpty)

    def meet(a: Set[T], b: Set[T]) = a.intersect(b)

}
