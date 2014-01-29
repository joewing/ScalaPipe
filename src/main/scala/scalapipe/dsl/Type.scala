package scalapipe.dsl

import scalapipe.{LabelMaker, ValueType}

abstract class Type(val name: String) {

    def this() = this(LabelMaker.getTypeLabel)

    override def toString = name

    private[scalapipe] def create(): ValueType

}
