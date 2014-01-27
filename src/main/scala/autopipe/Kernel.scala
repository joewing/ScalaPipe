package autopipe

import autopipe.dsl.AutoPipeBlock

private[autopipe] class Kernel(
        val ap: AutoPipe,
        val apb: AutoPipeBlock
    ) extends DebugInfo {

    private[autopipe] val name = apb.name
    private[autopipe] val index = LabelMaker.getInstanceIndex
    private[autopipe] val label = "instance" + index
    private[autopipe] var device: Device = null
    private var outputs = Map[PortName, Stream]()
    private var inputs = Map[PortName, Stream]()
    private var configs = Map[String, Literal]()

    collectDebugInfo

    def apply(): StreamList = new StreamList(ap, this)

    def apply(s: Stream): StreamList = {
        setInput(null, s)
        new StreamList(ap, this)
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
        new StreamList(ap, this)
    }

    def apply(args: Array[Stream]) = {
        for (a <- args) setInput(null, a)
        new StreamList(ap, this)
    }

    private[autopipe] def kernelType = ap.kernelType(name, device.platform)

    private[autopipe] def setInput(n: String, s: Stream) {
        val portName: PortName = if(n == null) new IntPortName(inputs.size)
                                 else new StringPortName(n)
        s.setDest(this, portName)
        inputs += (portName -> s)
    }

    private[autopipe] def setOutput(pn: PortName, s: Stream) {
        outputs += (pn -> s)
    }

    private[autopipe] def replaceInput(os: Stream, ns: Stream) {
        for (i <- inputs if i._2 == os) {
            val portName = i._1
            inputs += (portName -> ns)
            ns.setDest(this, portName)
            return
        }
        sys.error("internal error: old stream not found")
    }

    private[autopipe] def setConfig(n: String, v: Any) {
        val value = Literal.get(v)
        configs += (n -> value)
    }

    private[autopipe] def setConfigs(o: Kernel) {
        configs ++= o.configs
    }

    private[autopipe] def getConfig(n: String): Literal = {
        configs.get(n) match {
            case Some(v) => v
            case None => null
        }
    }

    private def getPort(pn: PortName, lst: Seq[KernelPort]): KernelPort = {
        pn match {
        case ip: IntPortName => lst(ip.name)
        case _ =>
            lst.find(i => i.name == pn) match {
                case Some(p)    => p
                case None       => null
            }
        }
    }

    private def getInput(pn: PortName): KernelPort = getPort(pn, apb.inputs)

    private def getOutput(pn: PortName) = getPort(pn, apb.outputs)

    private[autopipe] def inputName(pn: PortName) = getInput(pn).name

    private[autopipe] def inputType(pn: PortName) = getInput(pn).valueType

    private[autopipe] def outputName(pn: PortName) = getOutput(pn).name

    private[autopipe] def outputType(pn: PortName) = getOutput(pn).valueType

    private[autopipe] def inputIndex(pn: PortName): Int = pn match {
        case in: IntPortName => in.name
        case _ => apb.inputs.indexWhere(i => i.name == pn)
    }

    private[autopipe] def inputIndex(s: Stream): Int = {
        val matches = inputs.toList.filter { case (k, v) => v == s }
        inputIndex(matches.head._1)
    }

    private[autopipe] def outputIndex(pn: PortName): Int = pn match {
        case in: IntPortName => in.name
        case _ => apb.outputs.indexWhere(o => o.name == pn)
    }

    private[autopipe] def getInputs: List[Stream] = inputs.toList.map(_._2)

    private[autopipe] def getOutputs: List[Stream] = outputs.toList.map(_._2)

    private[autopipe] def getConfigs: List[(String, Literal)] = configs.toList

    private def getConfigString: String = {
        def getAssignment(x: (String, Literal)): String = x._1 + "=" + x._2
        def getStr(cs: List[(String, Literal)]): String = cs match {
            case Nil => ""
            case x :: Nil => getAssignment(x)
            case x :: y :: ys => getAssignment(x) + "," + getStr(y::ys)
        }
        getStr(configs.toList)
    }

    override def toString = name

    private[autopipe] def validate {

        if (inputs.size > apb.inputs.size) {
            Error.raise("too many inputs connected for " + name, this)
        } else if (inputs.size < apb.inputs.size) {
            Error.raise("too few inputs connected for " + name, this)
        }

        if (outputs.size > apb.outputs.size) {
            Error.raise("too many outputs connected for " + name, this)
        } else if (outputs.size < apb.outputs.size) {
            Error.raise("too few outputs connected for " + name, this)
        }

    }

}
