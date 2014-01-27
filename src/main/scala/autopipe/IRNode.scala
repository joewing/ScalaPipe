package autopipe

private object IRNodeCounter {

    private var count = 0

    def getId: Int = {
        count += 1;
        count
    }

}

private[autopipe] trait IRNode {

    val op: NodeType.Value = NodeType.invalid

    def links        = Seq[Int]()
    def srcs         = Seq[BaseSymbol]()
    def dests        = Seq[BaseSymbol]()

    final def symbols = dests ++ srcs

    def replaceLink(o: Int, n: Int): IRNode = this

    final def replace(o: BaseSymbol, n: BaseSymbol): IRNode =
        replaceSources(o, n).replaceDest(o, n)

    def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = this

    def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = this

    final def setDest(n: BaseSymbol): IRNode =
        if (!dests.isEmpty) replaceDest(dests.head, n) else this

    final def equivalent(o: Any): Boolean = o.toString == toString

}

private[autopipe] case class IRStart(
        val next:    Int = -1,
        val id:      Int = IRNodeCounter.getId
    ) extends IRNode {

    override def links = Seq(next)

    override def replaceLink(o: Int, n: Int): IRNode = {
        if (next == o) {
            copy(next = n)
        } else {
            this
        }
    }

    override def toString    = "start " + next

}

private[autopipe] case class IRNoOp(
        val id: Int = IRNodeCounter.getId
    ) extends IRNode {

    override def toString    = "nop"

}

private[autopipe] case class IRInstruction(
        override val op:      NodeType.Value,
        val dest:                BaseSymbol,
        val srca:                BaseSymbol,
        val srcb:                BaseSymbol = null,
        val id:                  Int = IRNodeCounter.getId
    ) extends IRNode {

    override def srcs = Seq(srca, srcb).filter(_ != null)

    override def dests = Seq(dest)

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        val a = if (this.srca == o) this.copy(srca = n) else this
        val b = if (    a.srcb == o)     a.copy(srcb = n) else a
        return b
    }

    override def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (dest == o) copy(dest = n) else this
    }

    override def toString = dest.toString + " <- " +
        (if (srcb != null) {
            srca.toString + " " + op.toString + " " + srcb.toString
        } else {
            if (op != NodeType.assign) {
                op.toString + " " + srca.toString
            } else {
                srca.toString
            }
        })

}

private[autopipe] case class IRStore(
        val dest:   BaseSymbol,
        val offset: BaseSymbol,
        val src:    BaseSymbol,
        val id:     Int = IRNodeCounter.getId
    ) extends IRNode {

    override def dests = Seq(dest)
    override def srcs = Seq(src, offset)

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        val a = if (this.offset == o) this.copy(offset = n) else this
        val b = if (   a.src    == o)    a.copy(src    = n) else a
        return b
    }

    override def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (dest == o) copy(dest = n) else this
    }

    override def toString = dest + "[" + offset + "] <- " + src

}

private[autopipe] case class IRLoad(
        val dest:   BaseSymbol,
        val src:    BaseSymbol,
        val offset: BaseSymbol,
        val id:     Int = IRNodeCounter.getId
    ) extends IRNode {

    override def dests = Seq(dest)
    override def srcs = Seq(src, offset)

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        val a = if (this.offset == o) this.copy(offset = n) else this
        val b = if (   a.src    == o)    a.copy(src    = n) else a
        return b
    }

    override def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (dest == o) copy(dest = n) else this
    }

    override def toString = dest + " <- " + src + "[" + offset + "]"

}

private[autopipe] case class IRGoto(
        val next:    Int = -1,
        val id:      Int = IRNodeCounter.getId
    ) extends IRNode {

    override def links = Seq(next)

    override def replaceLink(o: Int, n: Int): IRNode = {
        if (next == o) {
            copy(next = n)
        } else {
            this
        }
    }

    override def toString = "goto " + next

}

private[autopipe] case class IRStop(
        val id: Int = IRNodeCounter.getId
    ) extends IRNode {

    override def toString = "stop"

}

private[autopipe] case class IRReturn(
        val result: BaseSymbol,
        val id:      Int = IRNodeCounter.getId
    ) extends IRNode {

    override def srcs = Seq(result)

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (result == o) copy(result = n) else this
    }

    override def toString = {
        if (result != null) {
            "return " + result 
        } else {
            "return"
        }
    }

}

private[autopipe] case class IRConditional(
        val test:    BaseSymbol,
        val iTrue:  Int = -1,
        val iFalse: Int = -1
    ) extends IRNode {

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (test == o) copy(test = n) else this
    }

    override def links = Seq(iTrue, iFalse)

    override def replaceLink(o: Int, n: Int): IRNode = {
        if (iTrue == o && iFalse == o) {
            copy(iTrue = n, iFalse = n)
        } else if (iTrue == o) {
            copy(iTrue = n)
        } else if (iFalse == o) {
            copy(iFalse = n)
        } else {
            this
        }
    }

    override def srcs = Seq(test)

    override def toString = "if " + test +
        " then " + iTrue.toString +
        " else " + iFalse.toString

}

private[autopipe] case class IRSwitch(
        val test: BaseSymbol,
        val targets: Seq[(BaseSymbol, Int)]
    ) extends IRNode {

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (srcs.contains(o)) {
            val newTest = if (test == o) copy(test = n) else this
            val newTargets = targets.map { case (s, t) =>
                if (s == o) (n, t) else (s, t)
            }
            newTest.copy(targets = newTargets)
        } else {
            this
        }
    }

    override def links = targets.map(_._2)

    override def replaceLink(o: Int, n: Int): IRNode = {
        if (links.contains(o)) {
            val newTargets = targets.map { case (s, l) =>
                if (l == o) (s, n) else (s, l)
            }
            copy(targets = newTargets)
        } else {
            this
        }
    }

    override def srcs = test +: targets.filter(_._1 != null).map(_._1)

    override def toString = "switch " + test +
        targets.foldLeft("") { (a, b) =>
            if (b._1 == null) {
                a + " default: " + b._2
            } else {
                a + " case " + b._1 + ": " + b._2
            }
        }

}

private[autopipe] case class IRCall(
        val func: String,
        val args: Seq[BaseSymbol] = Seq(),
        val dest: BaseSymbol = null
    ) extends IRNode {

    override def dests = Seq(dest)
    override def srcs = args

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (args.contains(o)) {
            val newArgs = args.map(a => if (a == o) n else a)
            copy(args = newArgs)
        } else {
            this
        }
    }

    override def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (dest == o) copy(dest = n) else this
    }

    override def toString = func + "(" + args.mkString(", ") + ")"

}

private[autopipe] case class IRPhi(
        val symbol: BaseSymbol,
        val dest: BaseSymbol = null,
        val inputs: Map[Int, BaseSymbol] = Map()
    ) extends IRNode {

    override def dests = Seq(dest)
    override def srcs = inputs.map(_._2).toSeq

    override def replaceSources(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (srcs.contains(o)) {
            val newInputs = inputs.map { case (k, v) =>
                if (v == o) (k, n) else (k, v)
            }
            copy(inputs = newInputs)
        } else {
            this
        }
    }

    override def replaceDest(o: BaseSymbol, n: BaseSymbol): IRNode = {
        if (dest == o) copy(dest = n) else this
    }

    override def toString = dest + " <= phi(" +
        inputs.foldLeft("") { (a, i) =>
            a + (if (a.isEmpty) "" else ", ") + i._2 + "@" + i._1
        } + ")"

}
