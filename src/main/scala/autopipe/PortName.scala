package autopipe

private[autopipe] abstract class PortName

private[autopipe] class StringPortName(val name: String) extends PortName {
    override def toString = name
}

private[autopipe] class IntPortName(val name: Int) extends PortName {
    override def toString = "$" + name
}
