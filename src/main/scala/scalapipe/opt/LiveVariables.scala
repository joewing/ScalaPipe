package scalapipe.opt

import scalapipe._

object LiveVariables extends DataFlowProblem {

    type T = BaseSymbol

    def forward = false

    def init(kt: KernelType, graph: IRGraph) = Set[T]()

    def gen(sb: StateBlock, in: Set[T]): Set[T] =
        sb.srcs.filter(isVariable).toSet

    def kill(sb: StateBlock, in: Set[T]): Set[T] = {
        val dests = sb.nodes.flatMap { node =>
            node match {
                case st: IRStore        => Seq[T]()
                case _ => node.dests.filter(isVariable)
            }
        }
        dests.toSet
    }

    def meet(a: Set[T], b: Set[T]) = a.union(b)

}
