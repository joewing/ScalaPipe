
package autopipe.opt

import autopipe._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Stack

private[autopipe] class Dominators(_co: CodeObject, _graph: IRGraph)
        extends DominatorBase(_co, _graph) {

    val phis = new HashMap[BaseSymbol, HashMap[StateBlock, IRPhi]]

    def inputs(a: StateBlock) = graph.inLinks(a)

    def outputs(a: StateBlock) = graph.links(a)

    def root = graph.root

    /** Convert graph to static single assignment. */
/*
    def computeSSA() {
        frontiers()
        placePhis()
        rename()
    }
*/

    /** Destroy static single assignment. */
/*
    def destroySSA() {
        val phiList = graph.nodes.collect { case p: IRPhi => p }
        phiList.foreach { p =>
            p.inputs.foreach { i =>
                val block = i._1
                val symbol = i._2
                if (block.state == 0) {
                    block.replace(symbol, p.dest)
                } else if (block.state != -1) {
                    // This is a critical edge that needs to be split.
                    val ab = new StateBlock(new IRGoto(p.block))
                    val in = new IRInstruction(NodeType.assign, p.dest, symbol)
                    block.jump match {
                        case c: IRConditional =>
                            assert(c.iTrue == p.block || c.iFalse == p.block)
                            if (c.iTrue == p.block) {
                                c.iTrue = ab
                            }
                            if (c.iFalse == p.block) {
                                c.iFalse = ab
                            }
                        case s: IRSwitch =>
                            val toUpdate = s.targets.filter { _._2 == p.block }
                            s.targets --= toUpdate
                            s.targets ++= toUpdate.map { o => (o._1, ab) }
                        case g: IRGoto =>
                            assert(g.next == p.block)
                            g.next = ab
                    }
                    ab.state = -1
                    ab.insert(in)                    
                    graph.insert(ab)
                    val otherPhis = p.block.nodes.collect {
                        case l: IRPhi if l != p => l
                    }
                    otherPhis.foreach { o =>
                        val ninputs = new HashMap[StateBlock, BaseSymbol]
                        o.inputs.foreach { case t@(k, v) =>
                            if (k == block) {
                                ninputs += ((ab, v))
                            } else {
                                ninputs += t
                            }
                        }
                        o.inputs.clear
                        o.inputs ++= ninputs
                    }
                } else {
                    // Insert an extra assignment.
                    val in = new IRInstruction(NodeType.assign, p.dest, symbol)
                    block.insert(in)
                }
            }
            graph.remove(p)
        }
    }
*/

/*
    private def canRename(s: BaseSymbol): Boolean = s match {
        case p: PortSymbol => false
        case _ => !s.valueType.isInstanceOf[ArrayValueType]
    }
*/

/*
    private def placePhis() {
        val live = LiveVariables.solve(co, graph)
        val symbols = new HashSet[BaseSymbol]
        graph.blocks.foreach { b =>
            symbols ++= b.symbols.filter { s => canRename(s) }
        }
        for (s <- symbols) {
            phis(s) = new HashMap[StateBlock, IRPhi]
            placePhis(live, s)
        }
    }
*/

/*
    private def placePhis(live: HashMap[StateBlock, Set[BaseSymbol]],
                                 s: BaseSymbol) {

        val processed  = new HashMap[StateBlock, Boolean]
        val work         = new HashSet[StateBlock]
        val hasPhi      = new HashMap[StateBlock, Boolean]

        def add(b: StateBlock) {
            if (!processed(b)) {
                work += b
                processed(b) = true
            }
        }

        graph.blocks.foreach { b =>
            hasPhi(b) = false
            processed(b) = false
            if (b.dests.contains(s)) {
                add(b)
            }
        }

        while (!work.isEmpty) {
            val x = work.head
            work -= x
            for (y <- df(x) if !hasPhi(y) && live(y).contains(s)) {
                hasPhi(y) = true
                phis(s)(y) = new IRPhi(s)
                add(y)
            }
        }

    }
*/

/*
    private def renameSymbol(start: StateBlock, s: BaseSymbol) {

        def helper(x: StateBlock, current: BaseSymbol) {

            // If there is a phi at this node, we need to use the output
            // of the phi.
            var newSymbol = phis(s).get(x) match {
                case Some(p) =>
                    p.dest = co.createTemp(s.valueType)
                    p.dest
                case None =>
                    current
            }

            // Update all sources that use this symbol.
            for (n <- x.nodes if !n.isInstanceOf[IRPhi]) {
                n.replaceSources(s, newSymbol)
            }

            // If we are writting to the symbol, we need to create
            // a new temporary for the destination.
            for (n <- x.nodes if n.dests.contains(s)) {
                n.dest = co.createTemp(s.valueType)
                newSymbol = n.dest
            }

            // Update any phis that take input from this node.
            for (l <- x.links) {
                phis(s).get(l) match {
                    case Some(p) =>
                        p.inputs(x) = newSymbol
                    case None => null
                }
            }

            // Update the children we dominate.
            for (c <- ilist(x)) {
                helper(c, newSymbol)
            }

        }

        helper(start, s)

    }
*/

/*
    private def rename() {

        // Insert phis.
        for (s <- phis) {
            var symbol = s._1
            for (n <- s._2) {
                var block = n._1
                var phi = n._2
                for (i <- graph.inLinks(block)) {
                    phi.inputs += ((i, symbol))
                }
                block.insert(phi)
            }
        }

        // Get a set of all symbols to rename.
        val symbols = new HashSet[BaseSymbol]
        graph.blocks.foreach { node =>
            symbols ++= node.symbols.filter { s => canRename(s) }
        }

        for (s <- symbols) {
            renameSymbol(graph.root, s)
        }

    }
*/

}

