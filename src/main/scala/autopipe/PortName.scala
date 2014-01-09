
package autopipe

private[autopipe] abstract class PortName {
    def isIndex: Boolean
    def index: Int
}

private[autopipe] class StringPortName(name: String) extends PortName {
    override def isIndex = false
    override def toString = name
    override def index = -1
}

private[autopipe] class IntPortName(name: Int) extends PortName {
    override def isIndex = true
    override def toString = "$" + name
    override def index = name
}

