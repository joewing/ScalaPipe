package scalapipe

private[scalapipe] abstract class PortName

private[scalapipe] class StringPortName(val name: String) extends PortName {

    override def equals(other: Any) = other match {
        case sp: StringPortName => name.equals(sp.name)
        case _                  => false
    }

    override def hashCode = name.hashCode

    override def toString = name

}

private[scalapipe] class IntPortName(val name: Int) extends PortName {

    override def equals(other: Any) = other match {
        case ip: IntPortName    => ip.name == name
        case _                  => false
    }

    override def hashCode = name

    override def toString = "$" + name
}
