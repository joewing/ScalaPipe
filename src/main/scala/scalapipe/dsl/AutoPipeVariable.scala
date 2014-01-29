package scalapipe.dsl

import scalapipe._

class AutoPipeVariable private[scalapipe] (
        val name: String,
        val k: Kernel
    ) {

    private[scalapipe] def create() = ASTSymbolNode(name, k)

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
