package scalapipe.dsl

import scala.language.dynamics
import scalapipe._

class Variable private[scalapipe] (
        private[scalapipe] val name: String,
        private[scalapipe] val kernel: Kernel
    ) extends Dynamic {

    private[scalapipe] def create() = ASTSymbolNode(name, kernel)

    def update(index: ASTNode, value: ASTNode): ASTAssignNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, kernel)
    }

    def update(index: Symbol, value: ASTNode): ASTAssignNode = {
        val symbol = create()
        symbol.apply(index)
        ASTAssignNode(symbol, value, kernel)
    }

    // Allow "x" to be a field (otherwise there's a conflict).
    def x: ASTSymbolNode = {
        val symbol = create()
        symbol.apply(SymbolLiteral("x", kernel))
    }

    def selectDynamic(name: String): ASTSymbolNode = {
        val symbol = create()
        symbol.apply(SymbolLiteral(name, kernel))
    }

    def updateDynamic(name: String)(value: ASTNode): ASTAssignNode = {
        val symbol = create()
        symbol.apply(SymbolLiteral(name, kernel))
        ASTAssignNode(symbol, value, kernel)
    }

}
