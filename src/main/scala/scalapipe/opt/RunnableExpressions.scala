package scalapipe.opt

import scalapipe._

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

    private def disjoint(a: BaseSymbol, b: BaseSymbol): Boolean  = {
        (a, b) match {
            case (ia: ImmediateSymbol, ib: ImmediateSymbol) =>
                ia.value != ib.value
            case _ => false
        }
    }

    private def hasSymbolConflict(a: IRNode, b: IRNode) = {
        !a.dests.intersect(b.dests).isEmpty ||
        !b.srcs.intersect(a.dests).isEmpty ||
        !a.srcs.intersect(b.dests).isEmpty
    }

    private def hasPortConflict(a: IRNode, b: IRNode) = {
        val aInputs = a.srcs.collect { case is: InputSymbol => is }
        val bInputs = b.srcs.collect { case is: InputSymbol => is }
        val aOutputs = a.dests.collect { case os: OutputSymbol => os }
        val bOutputs = b.dests.collect { case os: OutputSymbol => os }
        (!aInputs.isEmpty && !bOutputs.isEmpty) ||
        (!bInputs.isEmpty && !aOutputs.isEmpty) ||
        !aInputs.intersect(bInputs).isEmpty ||
        !aOutputs.intersect(bOutputs).isEmpty
    }

    private def hasConflict(a: IRNode, b: IRNode): Boolean = (a, b) match {
        case (la: IRLoad, lb: IRLoad) if la.flat && lb.flat =>
            la.offset == lb.dest || lb.offset == la.dest || la.dest == lb.dest
        case (sa: IRStore, lb: IRLoad) if sa.flat && lb.flat =>
            (sa.dest == lb.src && !disjoint(sa.offset, lb.offset)) ||
            sa.src == lb.dest || sa.offset == lb.dest
        case (la: IRLoad, sb: IRStore) if la.flat && sb.flat =>
            (sb.dest == la.src && !disjoint(sb.offset, la.offset)) ||
            sb.src == la.dest || sb.offset == la.dest
        case (sa: IRStore, sb: IRStore) if sa.flat && sb.flat =>
            sa.dest == sb.dest && !disjoint(sa.offset, sb.offset)
        case (sa: IRStore, _) =>
            !sa.flat || !sa.symbols.intersect(b.symbols).isEmpty
        case (la: IRLoad, _) =>
            !la.flat || !la.symbols.intersect(b.symbols).isEmpty
        case (_, sb: IRStore) =>
            !sb.flat || !sb.symbols.intersect(b.symbols).isEmpty
        case (_, lb: IRLoad) =>
            !lb.flat || !lb.symbols.intersect(b.symbols).isEmpty
        case _ =>
            hasSymbolConflict(a, b) || hasPortConflict(a, b)
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
