package scalapipe

private[scalapipe] abstract class PortName

private[scalapipe] class StringPortName(val name: String) extends PortName {
    override def toString = name
}

private[scalapipe] class IntPortName(val name: Int) extends PortName {
    override def toString = "$" + name
}
