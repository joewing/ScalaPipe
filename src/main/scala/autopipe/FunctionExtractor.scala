
package autopipe

import autopipe.dsl.AutoPipeFunction
import autopipe.dsl.AutoPipeObject

private[autopipe] object FunctionExtractor {

    def functions(node: ASTNode): Seq[AutoPipeFunction] = node match {
        case call: ASTCallNode  =>
            call.children.flatMap(functions) ++ List(call.func)
        case node: ASTNode        => node.children.flatMap(functions)
        case _                        => Nil
    }

    def objects(node: ASTNode): Seq[AutoPipeObject] = node match {
        case sp: ASTSpecial  =>
            sp.children.flatMap(objects) ++ List(sp.obj)
        case node: ASTNode    => node.children.flatMap(objects)
        case _                    => Nil
    }

}

