package scalapipe

import scalapipe.dsl.Kernel

object DSLHelper {

    def ifThen[T](cond: Boolean, thenp: => T) = {
        if (cond) thenp
    }

    def ifThenElse[T](cond: Boolean, thenp: => T, elsep: => T): T = {
        if (cond) thenp else elsep
    }

    def getType(k: Kernel, name: String): ValueType = {
        val syms: Seq[KernelSymbol] = k.states ++
                                      k.inputs ++
                                      k.outputs ++
                                      k.configs
        syms.filter(_.name == name).headOption match {
            case Some(s) =>
                return s.valueType
            case _ =>
                Error.raise(s"symbol not found: $name", k)
                return ValueType.void
        }
    }

    def getType(k: Kernel, node: ASTNode): ValueType = node match {
        case lt: Literal =>
            lt.valueType
        case sn: ASTSymbolNode =>
            getType(k, sn.symbol)
        case null =>
            Error.raise("invalid expression", k)
            ValueType.void
        case _ =>
            getType(k, node.parent)
    }

}
