
package autopipe.opt

import autopipe._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Stack

private[autopipe] abstract class DominatorBase(val co: CodeObject,
                                                              val graph: IRGraph) {

    // DFS
    var count        = 0
    val child        = new HashMap[StateBlock, StateBlock]
    val sibling     = new HashMap[StateBlock, StateBlock]
    val dfn          = new HashMap[StateBlock, Int]
    val parent      = new HashMap[StateBlock, StateBlock]
    val vertex      = new HashMap[Int, StateBlock]
    val progeny     = new HashMap[StateBlock, Int]

    // Semidominators
    val sdom         = new HashMap[StateBlock, Int]
    val slist        = new HashMap[StateBlock, HashSet[StateBlock]]
    val edges        = new HashMap[StateBlock, HashSet[StateBlock]]
    val ancestor    = new HashMap[StateBlock, StateBlock]

    // Dominators
    val idom         = new HashMap[StateBlock, StateBlock]

    // Frontiers.
    val ilist        = new HashMap[StateBlock, HashSet[StateBlock]]
    val df            = new HashMap[StateBlock, HashSet[StateBlock]]

    dfst()
    semidominators()
    dominators()

    def inputs(a: StateBlock): List[StateBlock]

    def outputs(a: StateBlock): List[StateBlock]

    def root: StateBlock

    /** Determine if a dominates b. */
    def dominates(a: StateBlock, b: StateBlock): Boolean = {
        var d = idom(b)
        while (d != root && d != null) {
            if (d == a) {
                return true
            }
            d = idom(d)
        }
        return a == root
    }

    /** Compute the depth-first spanning tree. */
    private def dfst() {
        def dfs(x: StateBlock) {
            count += 1
            dfn(x) = count
            vertex(count) = x
            for (y <- outputs(x) if dfn(y) == 0) {
                parent(y) = x
                sibling(y) = child(x)
                child(x) = y
                dfs(y)
            }
            progeny(x) = count - dfn(x)
        }
        graph.blocks.foreach { z =>
            child(z) = null
            dfn(z) = 0
        }
        parent(root) = null
        dfs(root)
    }

    private def eval(x: StateBlock): StateBlock = {
        var sneakiest = Int.MaxValue
        var accomplice: StateBlock = null
        var p = x
        while (p != null) {
            if (sdom(p) < sneakiest) {
                accomplice = p
                sneakiest = sdom(p)
            }
            p = ancestor(p)
        }
        return accomplice
    }

    /** Compute semidominators. */
    private def semidominators() {
        graph.blocks.foreach { x =>
            sdom(x) = dfn(x)
            ancestor(x) = null
            slist(x) = new HashSet[StateBlock]
            edges(x) = new HashSet[StateBlock]
        }
        for (n <- count to 2 by -1) {
            val y = vertex(n)
            for (x <- inputs(y)) {
                val a = eval(x)
                if (sdom(a) < sdom(y)) {
                    sdom(y) = sdom(a)
                }
            }
            slist(vertex(sdom(y))) += y
            ancestor(y) = parent(y)
        }
        graph.blocks.foreach { y =>
            if (sdom(y) > 0) {
                edges(vertex(sdom(y))) += y
            }
            val c = child(y)
            if (c != null) {
                edges(y) += c
                val s = sibling(c)
                if (s != null) {
                    edges(y) += s
                }
            }
        }
    }

    /** Compute dominators. */
    private def dominators() {

        graph.blocks.foreach { x =>
            ancestor(x) = null
            idom(x) = null
        }

        val sameDomAs = new HashMap[StateBlock, StateBlock]

        for (n <- count to 1 by -1) {
            val y = vertex(n)
            for (z <- slist(y)) {
                val t = eval(z)
                val s = sdom(t)
                if (s == n) {
                    idom(z) = y
                } else {
                    idom(z) = null
                    sameDomAs(z) = t
                }
            }
            val c = child(y)
            if (c != null) {
                ancestor(c) = y
                val s = sibling(c)
                if (s != null) {
                    ancestor(s) = y
                }
            }
        }
        for (n <- 2 to count) {
            val z = vertex(n)
            if (idom(z) == null) {
                idom(z) = idom(sameDomAs(z))
            }
        }
        idom(root) = null

    }

    /** Compute dominance frontiers. */
    protected def frontiers() {

        graph.blocks.foreach { b =>
            ilist(b) = new HashSet[StateBlock]
        }
        graph.blocks.foreach { b =>
            if (idom(b) != null) {
                ilist(idom(b)) += b
            }
        }

        for (n <- count to 1 by -1) {
            val x = vertex(n)
            df(x) = new HashSet[StateBlock]
            for (y <- outputs(x)) {
                if (idom(y) != x) {
                    df(x) += y
                }
            }
            for (z <- ilist(x)) {
                for (y <- df(z)) {
                    if (idom(y) != x) {
                        df(x) += y
                    }
                }
            }
            
        }

    }

}

