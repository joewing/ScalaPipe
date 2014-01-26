package autopipe.opt

import autopipe._

object LiveVariables extends DataFlowProblem {

    type T = BaseSymbol

    def forward = false

    def init(kt: KernelType, graph: IRGraph) = Set[T]()

    def gen(sb: StateBlock, in: Set[T]): Set[T] =
        Set(sb.srcs.filter(isVariable): _*)

    def kill(sb: StateBlock, in: Set[T]): Set[T] = {
        val dests = sb.nodes.flatMap { node =>
            node match {
                case vs: IRVectorStore  => List[T]()
                case as: IRArrayStore    => List[T]()
                case _ => node.dests.filter(isVariable)
            }
        }
        Set[T](dests: _*)
    }

    def meet(a: Set[T], b: Set[T]) = a.union(b)

}
