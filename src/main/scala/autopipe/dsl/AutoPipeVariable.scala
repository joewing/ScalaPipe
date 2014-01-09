
package autopipe.dsl

import autopipe._

class AutoPipeVariable private[autopipe] (val name: String,
                                                        val apb: AutoPipeBlock) {

    private[autopipe] def create() = ASTSymbolNode(name, apb)

    def update(index: ASTNode, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, apb)
    }

    def update(index: Symbol, value: ASTNode): ASTNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, apb)
    }

}

