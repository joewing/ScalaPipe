package autopipe

class ContantFolderSpec extends UnitSpec {

    "Folding contant integer expressions" should "create integers" in {

        def check(expected: Int, op: NodeType.Value, a: Int, b: Int = 0) {
            val a = IntLiteral(2, null)
            val b = IntLiteral(3, null)
            val c = IntLiteral(0, null)
            val dest = new ASTSymbolNode("dest")
            val expression = ASTOpNode(op, a, b)
            val statement = ASTAssignNode(dest, expression)
            val folder = new ConstantFolder(null)
            val actual = folder.fold(statement)
            assert(actual.isInstanceOf[IntLiteral])
            val lit = actual.asInstanceOf[IntLiteral]
            assert(lit.value == expected)
        }

        check(-5, NodeType.neg, 5)

    }

}
