package autopipe

import autopipe.dsl.AutoPipeBlock

private[autopipe] object Error {

    private[autopipe] var errorCount = 0
    private[autopipe] var warnCount = 0

    def raise(msg: String): String = {
        errorCount += 1
        println("ERROR: " + msg)
        "<error " + msg + ">"
    }

    def raise(msg: String, info: DebugInfo): String = {
        if (info != null) {
            val prefix = info.fileName + "[" + info.lineNumber + "]: "
            raise(prefix + msg)
        } else {
            raise(msg)
        }
    }

    def raise(msg: String, apb: AutoPipeBlock): String = {
        if (!apb.scopeStack.isEmpty) {
            val scope = apb.scopeStack.last
            if (!scope.conditions.isEmpty) {
                raise(msg, scope.conditions.head)
            } else if (!scope.bodies.isEmpty) {
                raise(msg, scope.bodies.head)
            } else {
                raise(msg)
            }
        } else {
            raise(msg)
        }
    }

    def raise(msg: String, co: CodeObject): String = co match {
        case i: InternalBlockType       => raise(msg, i.expression)
        case f: InternalFunctionType    => raise(msg, f.expression)
        case _                          => raise(msg)
    }

    def warn(msg: String) {
        warnCount += 1
        println("WARN: " + msg)
    }

    def exit {
        if (errorCount > 0) {
            sys.exit(-1)
        } else {
            sys.exit(0)
        }
    }

}

