package autopipe.opt

import autopipe._

object ReachingDefs extends DataFlowProblem {

    type T = (BaseSymbol, Int)

    def forward = true

    def init(kt: KernelType, graph: IRGraph) = Set[T]()

    def gen(sb: StateBlock, in: Set[T]): Set[T] =
        sb.dests.map(d => ((d, sb.label))).toSet

    def kill(sb: StateBlock, in: Set[T]): Set[T] = {
        in.filter { case t@(s, d) =>
            sb.nodes.exists { node =>
                node match {
                    case st: IRStore    => false
                    case n: IRNode      => n.dests.contains(s)
                    case _              => false
                }
            }
        }
    }

    def meet(a: Set[T], b: Set[T]) = a.union(b)

}
