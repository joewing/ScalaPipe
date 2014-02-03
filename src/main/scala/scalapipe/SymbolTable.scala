package scalapipe

import scala.collection.mutable.ListBuffer
import scalapipe.dsl.Kernel

private[scalapipe] class SymbolTable(kernel: Kernel) {

    private var symbols = Map[String, BaseSymbol]()
    private var freeTemps = Set[TempSymbol]()

    private[scalapipe] val inputs = new ListBuffer[InputSymbol]
    private[scalapipe] val outputs = new ListBuffer[OutputSymbol]
    private[scalapipe] val configs = new ListBuffer[ConfigSymbol]
    private[scalapipe] val states = new ListBuffer[StateSymbol]
    private[scalapipe] val temps = new ListBuffer[TempSymbol]

    private def add(n: String, s: BaseSymbol) {
        if (symbols.contains(n)) {
            Error.raise("duplicate symbol: " + n, kernel)
        } else {
            symbols += (n -> s)
        }
    }

    private def add(s: BaseSymbol): Unit = add(s.name, s)

    def add(o: SymbolTable) {
        symbols ++= o.symbols
        inputs ++= o.inputs
        outputs ++= o.outputs
        configs ++= o.configs
        states ++= o.states
        temps ++= o.temps
    }

    def addInput(name: String, vt: ValueType) {
        val s = new InputSymbol(name, vt, inputs.size)
        add(s)
        inputs += s
    }

    def addOutput(name: String, vt: ValueType) {
        val s = new OutputSymbol(name, vt, outputs.size)
        add(s)
        outputs += s
    }

    def setOutputType(vt: ValueType) {
        val name = outputs.head.name
        val s = new OutputSymbol(name, vt, 0)
        outputs.clear
        symbols += (name -> s)
        outputs += s
    }

    def addConfig(name: String, vt: ValueType, value: Literal) {
        val s = new ConfigSymbol(name, vt, value)
        add(s)
        configs +=  s
    }

    def addState(name: String, vt: ValueType, value: Literal) {
        val s = new StateSymbol(name, vt, value)
        add(s)
        states += s
    }

    def createTemp(vt: ValueType): TempSymbol = {
        val tl = freeTemps.filter(_.valueType == vt)
        if (tl.isEmpty) {
            val t = new TempSymbol(vt)
            temps += t
            t
        } else {
            val t = tl.head
            freeTemps -= t
            t
        }
    }

    def releaseTemp(s: BaseSymbol) {
        // We don't release temporaries since this prevents some optimizations.
    }

    def get(name: String): BaseSymbol = symbols.get(name) match {
        case Some(s)    => s
        case None       => null
    }

    def getType(name: String): ValueType = symbols.get(name) match {
        case Some(s)    => s.valueType
        case None       => null
    }

    def isInput(name: String): Boolean = get(name) match {
        case is: InputSymbol    => true
        case _                  => false
    }

    def isOutput(name: String): Boolean = get(name) match {
        case os: OutputSymbol   => true
        case _                  => false
    }

    def getInput(pn: PortName): InputSymbol = pn match {
        case ip: IntPortName =>
            if (ip.name >= 0 && ip.name < inputs.size) {
                inputs(ip.name)
            } else {
                null
            }
        case _ =>
            symbols.get(pn.toString) match {
                case Some(s) if s.isInstanceOf[InputSymbol] =>
                    s.asInstanceOf[InputSymbol]
                case _ => null
            }
    }

    def getOutput(pn: PortName): OutputSymbol = pn match {
        case ip: IntPortName =>
            if (ip.name >= 0 && ip.name < outputs.size) {
                outputs(ip.name)
            } else {
                null
            }
        case _ =>
            symbols.get(pn.toString) match {
                case Some(s) if s.isInstanceOf[OutputSymbol] =>
                    s.asInstanceOf[OutputSymbol]
                case _ => null
            }
    }

    def inputIndex(pn: PortName): Int = pn match {
        case ip: IntPortName => ip.name
        case _ => inputs.indexOf(getInput(pn))
    }

    def outputIndex(pn: PortName): Int = pn match {
        case ip: IntPortName => ip.name
        case _ => outputs.indexOf(getOutput(pn))
    }

    def getBaseOffset(name: String): Int = {
        val preceding = states.takeWhile(_ != name).map(_.valueType)
        preceding.filter(!_.flat).map(_.bytes).sum
    }

}
