package scalapipe

import scalapipe.dsl._
import scala.collection.mutable.HashMap

/** A simple DSL for generating IRGraphs to be used for testing. */
private[scalapipe] class IRBuilder extends KernelType(
        new ScalaPipe, "test",
        new SymbolTable(null),
        Platforms.HDL
    ) {

    private val blocks = new HashMap[Int, StateBlock]
    private var currentBlock: StateBlock = null

    private var labelCounter = 0

    def pure = true

    private def getLabel: Int = {
        val result = labelCounter
        labelCounter += 1
        result
    }

    private def getName: String = "l" + getLabel

    def input(t: Type): InputSymbol = {
        val i = new InputSymbol(getName, t.create(), inputs.size)
        inputs += i
        i
    }

    def output(t: Type): OutputSymbol = {
        val o = new OutputSymbol(getName, t.create(), outputs.size)
        outputs += o
        o
    }

    def state(t: Type, v: Any = null): StateSymbol = {
        val s = new StateSymbol(getName, t.create(), Literal.get(v))
        states += s
        s
    }

    def temp(t: Type, id: Int = getLabel): TempSymbol = {
        val s = new TempSymbol(t.create(), id)
        temps += s
        s
    }

    def literal(v: Any): ImmediateSymbol = {
        new ImmediateSymbol(Literal.get(v))
    }

    private def append(node: IRNode) {
        currentBlock = currentBlock.append(node)
        blocks(currentBlock.label) = currentBlock
    }

    private def get(i: Int): StateBlock = {
        blocks.getOrElseUpdate(i, StateBlock(label = i))
    }

    def continuous {
        currentBlock = currentBlock.copy(continuous = true)
        blocks(currentBlock.label) = currentBlock
    }

    def label(i: Int) {
        val sb = get(i)
        if (currentBlock != null) {
            currentBlock.jump match {
                case s: IRStart => ()
                case g: IRGoto => ()
                case s: IRStop => ()
                case c: IRConditional => ()
                case _ =>
                    currentBlock = currentBlock.append(IRGoto(sb.label))
                    blocks(currentBlock.label) = currentBlock
            }
        }
        currentBlock = sb
    }

    def start {
        start(1)
    }

    def start(l: Int) {
        append(IRStart(l))
    }

    def nop {
        append(IRNoOp())
    }

    def op(op: NodeType.Value,
           dest: BaseSymbol,
           srca: BaseSymbol,
           srcb: BaseSymbol = null) {
        append(IRInstruction(op, dest, srca, srcb))
    }

    def store(dest: BaseSymbol, offset: BaseSymbol, src: BaseSymbol) {
        append(IRStore(dest, offset, src))
    }

    def load(dest: BaseSymbol, offset: BaseSymbol, src: BaseSymbol) {
        append(IRLoad(dest, src, offset))
    }

    def goto(i: Int) {
        val sb = get(i)
        append(IRGoto(sb.label))
    }

    def stop {
        append(IRStop())
    }

    def ret(result: BaseSymbol) {
        append(IRReturn(result))
    }

    def cond(test: BaseSymbol, t: Int, f: Int) {
        val iTrue = get(t)
        val iFalse = get(f)
        append(IRConditional(test, iTrue.label, iFalse.label))
    }

    def call(func: String, args: List[BaseSymbol]) {
        append(IRCall(func, args))
    }

    def graph: IRGraph = {
        val first = blocks.filter(_._1 > 0).minBy(_._1)._2
        val updated = blocks.map { case (k, v) =>
            val next = blocks.get(k + 1) match {
                case Some(sb)  => sb.label
                case None        => first.label
            }
            val nb: StateBlock =
                v.jump match {
                    case s: IRStart =>
                        if (s.next == -1) {
                            v.replace(s, s.copy(next = next))
                        } else {
                            v
                        }
                    case s: IRStop => v
                    case g: IRGoto =>
                        if (g.next == -1) {
                            v.replace(g, g.copy(next = next))
                        } else {
                            v
                        }
                    case c: IRConditional => v
                    case _ => v.append(IRGoto(next))
                }
            (k, nb)
        }
        return updated.values.foldLeft(IRGraph()) { (g, sb) => g.insert(sb) }
    }

    override def emit(dir: java.io.File) {
    }

    def internal = true

}
