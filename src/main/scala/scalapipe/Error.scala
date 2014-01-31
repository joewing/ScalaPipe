package scalapipe

import scalapipe.dsl.Kernel

private[scalapipe] object Error {

    private var errorContexts = Set[String]()
    private[scalapipe] def errorCount = errorContexts.size

    private var warnContexts = Set[String]()
    private[scalapipe] def warnCount = warnContexts.size

    def raise(msg: String, prefix: String = ""): String = {
        val context = if (prefix.isEmpty) msg else prefix
        if (!errorContexts.contains(context)) {
            errorContexts += context
            println(s"ERROR: $prefix$msg")
        }
        s"<error $prefix$msg>"
    }

    def raise(msg: String, info: DebugInfo): String = {
        if (info != null) {
            val prefix = s"${info.fileName}[${info.lineNumber}]: "
            raise(msg, prefix)
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
        warnContexts += msg
        println(s"WARN: $msg")
    }

    def exit {
        println(s"$errorCount errors, $warnCount warnings")
        if (errorCount > 0) {
            sys.exit(-1)
        } else {
            sys.exit(0)
        }
    }

}

