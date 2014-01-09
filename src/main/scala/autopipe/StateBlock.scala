
package autopipe

import scala.collection.mutable.HashSet

private[autopipe] case class StateBlock(
        val nodes: List[IRNode] = Nil,
        val label: Int             = 0,
        val ast: ASTNode          = null,
        val continuous: Boolean = false
    ) {

    final def jump: IRNode = nodes.lastOption.getOrElse(null)

    final def dests        = nodes.flatMap(_.dests)
    final def srcs         = nodes.flatMap(_.srcs)
    final def symbols     = dests ++ srcs
    final def links        = jump.links

    final def replace(o: BaseSymbol, n: BaseSymbol): StateBlock = {
        copy(nodes = nodes.map(_.replace(o, n)))
    }

    final def replaceSources(o: BaseSymbol, n: BaseSymbol): StateBlock = {
        copy(nodes = nodes.map(_.replaceSources(o, n)))
    }

    final def replaceLink(o: Int, n: Int): StateBlock = {
        if (links.contains(o)) {
            copy(nodes = nodes.map(_.replaceLink(o, n)))
        } else {
            this
        }
    }

    final def remove(node: IRNode): StateBlock = {
        copy(nodes = nodes.filter(_ != node))
    }

    final def insert(node: IRNode): StateBlock = {
        copy(nodes = node :: nodes)
    }

    final def append(node: IRNode): StateBlock = {
        copy(nodes = nodes :+ node)
    }

    final def replace(o: IRNode, n: IRNode): StateBlock = {
        val newNodes = nodes.map { t =>
            if (o == t) n else t
        }
        copy(nodes = newNodes)
    }

    override def equals(arg: Any): Boolean = arg match {
        case b: StateBlock =>    b.nodes == nodes &&
                                        b.label == label &&
                                        b.continuous == continuous
        case _ => false
    }

    override def hashCode(): Int = label

    override def toString: String = {
        val labelString = "s" + label + (if (continuous) "c:" else ":")
        val nodeString = nodes.init.foldLeft("") { (a, node) =>
            a + "\t" + node.toString + "\n"
        }
        val jumpString = jump match {
            case gt: IRGoto if gt.next == label + 1 => ""
            case _ => "\t" + jump.toString + "\n"
        }
        if (nodeString.isEmpty && jumpString.isEmpty) {
            return labelString + "\n"
        } else {
            return labelString + nodeString + jumpString
        }
    }

    final def equivalent(o: Any): Boolean = o.toString == toString

}

