package scalapipe

import scalapipe.dsl.Func

private[scalapipe] object FunctionExtractor {

    def functions(node: ASTNode): Set[Func] = node match {
        case call: ASTCallNode =>
            call.children.flatMap(functions).toSet + call.func
        case node: ASTNode     => node.children.flatMap(functions).toSet
        case _                 => Set()
    }

}
