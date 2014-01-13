
package autopipe;

import scala.collection.mutable.HashMap

private[autopipe] class Block(
        ap: AutoPipe,
        val name: String
    ) extends DebugInfo {

    private[autopipe] val index = LabelMaker.getInstanceIndex
    private[autopipe] val label = "instance" + index
    private[autopipe] var blockType: BlockType = null
    private[autopipe] var device: Device = null
    private val outputs = new HashMap[PortName, Stream]
    private val inputs = new HashMap[PortName, Stream]
    private val configs = new HashMap[String, Literal]

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
        for (i <- inputs) {
            val portName = i._1
            val stream = i._2
            if (stream == os) {
                inputs(portName) = ns
                ns.setDest(this, portName)
                return
            }
        }
        sys.error("internal error: old stream not found")
    }

    private[autopipe] def setConfig(n: String, v: Any) {
        val value = Literal.get(v)
        configs += (n -> value)
    }

    private[autopipe] def setConfigs(o: Block) {
        for (c <- o.configs) {
            configs += c
        }
    }

    private[autopipe] def getConfig(n: String): Literal = {
        configs.get(n) match {
            case Some(v) => v
            case None => null
        }
    }

    private[autopipe] def inputName(pn: PortName): String = {
        if(pn.isIndex) {
            blockType.inputName(pn.index)
        } else {
            pn.toString
        }
    }

    private[autopipe] def outputName(pn: PortName): String = {
        if(pn.isIndex) {
            blockType.outputName(pn.index)
        } else {
            pn.toString
        }
    }

    private[autopipe] def inputIndex(pn: PortName): Int =
        blockType.inputIndex(pn)

    private[autopipe] def inputIndex(s: Stream): Int = {
        val matches = inputs.toList.filter { case (k, v) => v == s }
        inputIndex(matches.head._1)
    }

    private[autopipe] def outputIndex(pn: PortName): Int =
        blockType.outputIndex(pn)

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

    private[autopipe] def emit: String = {
        val decl = "    " + blockType.name + " " + label
        if(!configs.isEmpty)
            decl + "(" + getConfigString + ");\n"
        else
            decl + ";\n"
    }

    override def toString = name

    private[autopipe] def validate {

        if (inputs.size > blockType.inputs.size) {
            Error.raise("too many inputs connected for " + name, this)
        } else if (inputs.size < blockType.inputs.size) {
            Error.raise("too few inputs connected for " + name, this)
        }

        if (outputs.size > blockType.outputs.size) {
            Error.raise("too many outputs connected for " + name, this)
        } else if (outputs.size < blockType.outputs.size) {
            Error.raise("too few outputs connected for " + name, this)
        }

    }

}

