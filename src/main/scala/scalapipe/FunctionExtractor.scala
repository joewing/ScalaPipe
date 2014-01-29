package scalapipe

import scalapipe.dsl.Func

private[scalapipe] object FunctionExtractor {

    def functions(node: ASTNode): Seq[Func] = node match {
        case call: ASTCallNode => call.children.flatMap(functions) :+ call.func
        case node: ASTNode     => node.children.flatMap(functions)
        case _                 => Nil
    }

}
