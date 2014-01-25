package autopipe

import scala.collection.immutable.HashMap

private[autopipe] case class IRGraph(val blocks: List[StateBlock] = Nil) {

    def nodes = blocks.view.flatMap(_.nodes)

    def root = blocks.view.filter(_.label == 0).head

    // Compute a mapping from labels to blocks.
    private[this] lazy val labelMap = HashMap[Int, StateBlock](
        blocks.map(b => (b.label, b)): _*
    )

    // Compute a mapping from nodes to blocks.
    private[this] lazy val nodeMap = HashMap[IRNode, StateBlock](
        blocks.flatMap(b => b.nodes.map(n => (n, b))): _*
    )

    // Compute links.
    private[this] lazy val linkMap = HashMap[Int, List[StateBlock]](
        blocks.map(b =>
            (b.label, blocks.filter(o => b.links.contains(o.label)))
        ): _*
    )

    // Compute inLinks.
    private[this] lazy val inLinkMap = HashMap[Int, List[StateBlock]](
        blocks.map(b =>
            (b.label, blocks.filter(o => o.links.contains(b.label)))
        ): _*
    )

    def block(label: Int): StateBlock = labelMap(label)

    def block(node: IRNode): StateBlock = nodeMap(node)

    def links(label: Int): List[StateBlock] = linkMap(label)

    def links(b: StateBlock): List[StateBlock] = links(b.label)

    def inLinks(label: Int): List[StateBlock] = inLinkMap(label)

    def inLinks(b: StateBlock): List[StateBlock] = inLinks(b.label)

    def remove(b: StateBlock): IRGraph = {
        val next = b.links.head
        val updated = blocks.filter(_ != b).map(_.replaceLink(b.label, next))
        IRGraph(updated)
    }

    def remove(n: IRNode): IRGraph = {
        val oldBlock = block(n)
        val newBlock = oldBlock.remove(n)
        if (newBlock.nodes.size == 1) {
            val next = newBlock.links.head
            val newBlocks = blocks.flatMap { b =>
                if (b == oldBlock) {
                    newBlock.jump match {
                        case gt: IRGoto if gt.next != newBlock.label => None
                        case _ => Some(newBlock)
                    }
                } else {
                    Some(b.replaceLink(oldBlock.label, next))
                }
            }
            IRGraph(newBlocks)
        } else {
            return update(newBlock)
        }
    }

    def insert(b: StateBlock): IRGraph = IRGraph(blocks :+ b)

    def update(b: StateBlock): IRGraph = {
        IRGraph(blocks.map(t => if (t.label == b.label) b else t))
    }

    def replace(o: StateBlock, n: StateBlock): IRGraph = {
        val updatedBlocks = blocks.map { t =>
            if (t.label == o.label) {
                n.replaceLink(o.label, n.label)
            } else {
                t.replaceLink(o.label, n.label)
            }
        }
        IRGraph(updatedBlocks)
    }

    /** Move node n to state block b. */
    def move(n: IRNode, b: StateBlock): IRGraph = {
        if (block(n) != b) {
            val afterRemove = remove(n)
            val newBlock = afterRemove.block(b.label)
            return afterRemove.update(newBlock.insert(n))
        } else {
            return this
        }
    }

    def replace(o: IRNode, n: IRNode): IRGraph = {
        if (o != n) {
            val oldBlock = block(o)
            update(oldBlock.replace(o, n))
        } else {
            this
        }
    }

    def validate {
        blocks.foreach { b =>
            assert(!b.links.isEmpty)
            assert(b.links.forall(l => blocks.exists(t => t.label == l)))
            assert(block(b.label) == b)
            b.nodes.foreach { n => assert(block(n) == b) }
            links(b).foreach { l => assert(inLinks(l).contains(b)) }
            inLinks(b).foreach { i => assert(links(i).contains(b)) }
        }
    }

    override def toString = blocks.sortBy(_.label).mkString

    final def equivalent(o: Any): Boolean = toString == o.toString

}
