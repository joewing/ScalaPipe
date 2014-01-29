package autopipe

import autopipe.dsl.Kernel

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

    def raise(msg: String, kernel: Kernel): String = {
        if (!kernel.scopeStack.isEmpty) {
            val scope = kernel.scopeStack.last
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

    def raise(msg: String, kt: KernelType): String = kt match {
        case ikt: InternalKernelType    => raise(msg, ikt.expression)
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

