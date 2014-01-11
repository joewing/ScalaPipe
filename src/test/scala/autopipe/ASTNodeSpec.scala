
package autopipe

import org.scalatest._

class ASTNodeSpec extends FlatSpec with Matchers {

    "Adding" should "produce a new node" in {
        val left = ASTSymbolNode("a")
        val right = ASTSymbolNode("b")
        val result = left + right
        assert(result.op == NodeType.add)
        assert(result.isInstanceOf[ASTOpNode])
        val op = result.asInstanceOf[ASTOpNode]
        assert(op.a == left)
        assert(op.b == right)
    }

    "Subtracting" should "produce a new node" in {
        val left = ASTSymbolNode("a")
        val right = ASTSymbolNode("b")
        val result = left - right
        assert(result.op == NodeType.sub)
        assert(result.isInstanceOf[ASTOpNode])
        val op = result.asInstanceOf[ASTOpNode]
        assert(op.a == left)
        assert(op.b == right)
    }


}
