package autopipe.dsl

import autopipe._

class AutoPipeVariable private[autopipe] (
        val name: String,
        val k: Kernel
    ) {

    private[autopipe] def create() = ASTSymbolNode(name, k)

    def update(index: ASTNode, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, k)
    }

    def update(index: Symbol, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, k)
    }

}
