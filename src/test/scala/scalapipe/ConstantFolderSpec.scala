package scalapipe

class ContantFolderSpec extends UnitSpec {

    "Folding contant integer expressions" should "create integers" in {

        def check(expected: Int, op: NodeType.Value, ia: Int, ib: Int = 0) {
            val a = IntLiteral(ia, null)
            val b = IntLiteral(ib, null)
            val dest = new ASTSymbolNode("dest")
            val expression = ASTOpNode(op, a, b)
            val statement = ASTAssignNode(dest, expression)
            val folder = new ConstantFolder(null)
            val actual = folder.fold(statement).asInstanceOf[ASTAssignNode].src
            assert(actual.isInstanceOf[IntLiteral])
            val lit = actual.asInstanceOf[IntLiteral]
            if(lit.value != expected) {
                fail("got " + lit.value.toString + " expected " + expected)
            }
        }

        check(-5, NodeType.neg, 5)
        check(~5, NodeType.compl, 5)
        check(4, NodeType.and, 6, 12)

    }

}
