package autopipe

import autopipe.dsl.AutoPipeFunction

private[autopipe] object FunctionExtractor {

    def functions(node: ASTNode): Seq[AutoPipeFunction] = node match {
        case call: ASTCallNode => call.children.flatMap(functions) :+ call.func
        case node: ASTNode     => node.children.flatMap(functions)
        case _                 => Nil
    }

}
