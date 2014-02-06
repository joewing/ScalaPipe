package scalapipe

import scalapipe.dsl.{Kernel, Stream, StreamList}

private[scalapipe] class KernelInstance(
        val sp: ScalaPipe,
        val kernel: Kernel
    ) extends DebugInfo {

    private[scalapipe] val name = kernel.name
    private[scalapipe] val index = LabelMaker.getInstanceIndex
    private[scalapipe] val label = "instance" + index
    private[scalapipe] var device: Device = null
    private var outputs = Map[PortName, Stream]()
    private var inputs = Map[PortName, Stream]()
    private[scalapipe] var configs = Map[String, Literal]()

    collectDebugInfo

    def apply(): StreamList = new StreamList(sp, this)

    def apply(s: Stream): StreamList = {
        setInput(null, s)
        new StreamList(sp, this)
    }

    def apply(args: (Symbol, Any)*) = {
        for(a <- args) {
            val name = if(a._1 == null) null else a._1.name
            if(a._2.isInstanceOf[Stream]) {
                setInput(name, a._2.asInstanceOf[Stream])
            } else if(name != null) {
                setConfig(name, a._2)
            }
        }
        new StreamList(sp, this)
    }

    def apply(args: Array[Stream]) = {
        for (a <- args) setInput(null, a)
        new StreamList(sp, this)
    }

    private[scalapipe] def kernelType = sp.kernelType(name, device.platform)

    private[scalapipe] def setInput(n: String, s: Stream) {
        val portName: PortName = if(n == null) new IntPortName(inputs.size)
                                 else new StringPortName(n)
        s.setDest(this, portName)
        inputs += (portName -> s)
    }

    private[scalapipe] def setOutput(pn: PortName, s: Stream) {
        outputs += (pn -> s)
    }

    private[scalapipe] def replaceInput(os: Stream, ns: Stream) {
        for (i <- inputs if i._2 == os) {
            val portName = i._1
            inputs += (portName -> ns)
            ns.setDest(this, portName)
            return
        }
        sys.error("internal error: old stream not found")
    }

    private[scalapipe] def setConfig(n: String, v: Any) {
        val value = Literal.get(v)
        configs += (n -> value)
    }

    private[scalapipe] def setConfigs(o: KernelInstance) {
        configs ++= o.configs
    }

    private[scalapipe] def getConfig(n: String): Literal = {
        configs.get(n) match {
            case Some(v) => v
            case None => null
        }
    }

    private def getPort(pn: PortName,
                        lst: Seq[KernelPort]): Option[KernelPort] = {
        pn match {
            case ip: IntPortName if ip.name < lst.size => Some(lst(ip.name))
            case _ => lst.find(_.name == pn)
        }
    }

    private def getInput(pn: PortName) = getPort(pn, kernel.inputs)

    private def getOutput(pn: PortName) = getPort(pn, kernel.outputs)

    private[scalapipe] def inputName(pn: PortName) = getInput(pn) match {
        case Some(i)    => i.name
        case _          => null
    }

    private[scalapipe] def inputType(pn: PortName) = getInput(pn) match {
        case Some(i)    => i.valueType
        case _          => ValueType.void
    }

    private[scalapipe] def outputName(pn: PortName) = getOutput(pn) match {
        case Some(o)    => o.name
        case _          => null
    }

    private[scalapipe] def outputType(pn: PortName) = getOutput(pn) match {
        case Some(o)    => o.valueType
        case _          => ValueType.void
    }

    private[scalapipe] def inputIndex(pn: PortName): Int = pn match {
        case in: IntPortName => in.name
        case _ => kernel.inputs.indexWhere(_.name == pn)
    }

    private[scalapipe] def inputIndex(s: Stream): Int = {
        val matches = inputs.toList.filter { case (k, v) => v == s }
        inputIndex(matches.head._1)
    }

    private[scalapipe] def outputIndex(pn: PortName): Int = pn match {
        case in: IntPortName => in.name
        case _ => kernel.outputs.indexWhere(_.name == pn)
    }

    private[scalapipe] def getInputs: List[Stream] = inputs.toList.map(_._2)

    private[scalapipe] def getOutputs: List[Stream] = outputs.toList.map(_._2)

/*
    private[scalapipe] def getConfigs: List[(String, Literal)] = configs.toList
*/

    override def toString = name

    private[scalapipe] def validate {

        if (inputs.size > kernel.inputs.size) {
            Error.raise("too many inputs connected for " + name, this)
        } else if (inputs.size < kernel.inputs.size) {
            Error.raise("too few inputs connected for " + name, this)
        }

        if (outputs.size > kernel.outputs.size) {
            Error.raise("too many outputs connected for " + name, this)
        } else if (outputs.size < kernel.outputs.size) {
            Error.raise("too few outputs connected for " + name, this)
        }

    }

}
