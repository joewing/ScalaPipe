
package autopipe

import autopipe.dsl.AutoPipeBlock

private[autopipe] object Error {

    def raise(msg: String): Nothing = {
        println("ERROR: " + msg)
        sys.exit(-1)
    }

    def raise(msg: String, node: ASTNode): Nothing = {
        if (node != null) {
            val prefix = node.fileName + "[" + node.lineNumber + "]: "
            raise(prefix + msg)
        } else {
            sys.error(msg)
        }
    }

    def raise(msg: String, apb: AutoPipeBlock): Nothing = {

        if (!apb.scopeStack.isEmpty) {
            val scope = apb.scopeStack.last
            if (!scope.conditions.isEmpty) {
                raise(msg, scope.conditions.head)
            } else if (!scope.bodies.isEmpty) {
                raise(msg, scope.bodies.head)
            } else {
                sys.error(msg)
            }
        } else {
            sys.error(msg)
        }

    }

    def raise(msg: String, co: CodeObject): Nothing = co match {
        case i: InternalBlockType      => raise(msg, i.expression)
        case f: InternalFunctionType  => raise(msg, f.expression)
        case _                                => raise(msg)
    }

    def warn(msg: String) {
        println("WARN: " + msg)
    }

}

