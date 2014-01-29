package scalapipe.dsl

import scalapipe._

class Variable private[scalapipe] (
        private[scalapipe] val name: String,
        private[scalapipe] val kernel: Kernel
    ) {

    private[scalapipe] def create() = ASTSymbolNode(name, kernel)

    def update(index: ASTNode, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, kernel)
    }

    def update(index: Symbol, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, kernel)
    }

}
