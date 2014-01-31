package scalapipe.dsl

import scalapipe.{LabelMaker, ValueType, SymbolValidator, DebugInfo}

abstract class Type(val name: String) extends DebugInfo {

    collectDebugInfo
    validateTypeName

    def this() = this(LabelMaker.getTypeLabel)

    override def toString = name

    private[scalapipe] def create(): ValueType

    protected def validateTypeName {
        SymbolValidator.validateType(name, this)
    }

}
