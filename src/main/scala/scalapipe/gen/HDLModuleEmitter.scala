package scalapipe.gen

import scalapipe._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

private[gen] class HDLModuleEmitter(
        protected val kt: KernelType,
        protected val graph: IRGraph
    ) extends HDLGenerator {

    private class Part(val state: Int, val args: Seq[String])

    private class Component(
        val name: String,
        val instanceName: String,
        val argCount: Int,
        val width: Int
    ) {
        var parts = Set[Part]()
    }

    private class SimpleComponent(val name: String,
                                  val instanceName: String,
                                  val args: Seq[String],
                                  val width: Int)

    private class StateCondition(val state: Int)

    private class AssignState(_state: Int, val value: String)
        extends StateCondition(_state)

    private class Assignment(val port: String) {
        var states = Set[AssignState]()
    }

    private val share = kt.parameters.get[Int]('share)

    private val components = new HashMap[String, Component]
    private val simpleComponents = new HashMap[String, SimpleComponent]
    private val componentIds = new HashMap[String, Int]

    private val readStates = new HashMap[String, Assignment]
    private val writeStates = new HashMap[String, Assignment]

    private var ramOffset = 0
    private val ramOffsetMap = new HashMap[BaseSymbol, Int]

    private var assignments = Set[String]()
    private var phis = Set[IRPhi]()
    private val guards = new HashMap[Int, ArrayBuffer[String]]

    def create(name: String, width: Int, state: Int,
               args: Seq[String]): String = {
        val baseIndex = name
        val instanceName: String = share match {
            case 0 =>    // No sharing
                val i = componentIds.getOrElseUpdate(baseIndex, 0)
                componentIds.update(baseIndex, i + 1)
                baseIndex + "x" + i.toString
            case 1 =>    // Share if in different states
                val idIndex = baseIndex + "s" + state
                val i = componentIds.getOrElseUpdate(idIndex, 0)
                componentIds.update(idIndex, i + 1)
                baseIndex + "x" + i.toString
            case 2 =>    // Complete sharing
                baseIndex + "x"
        }
        val comp = components.getOrElseUpdate(instanceName, {
            new Component(name, instanceName, args.size, width)
        })
        comp.parts += new Part(state, args)
        addGuard(state, "(last_state == state)")
        addGuard(state, instanceName + "_ready")
        return instanceName + "_result"
    }

    def createSimple(name: String, width: Int, args: Seq[String]): String = {
        val i = componentIds.getOrElseUpdate(name, 0)
        componentIds.update(name, i + 1)
        val instanceName = name + "x" + i.toString
        val comp = simpleComponents.getOrElseUpdate(instanceName, {
            new SimpleComponent(name, instanceName, args, width)
        })
        return instanceName + "_result"
    }

    def addGuard(state: Int, guard: String) {
        val gl = guards.getOrElseUpdate(state, { new ArrayBuffer[String] })
        gl += guard
    }

    def getRAMOffset(symbol: BaseSymbol): Int = {
        ramOffsetMap.getOrElseUpdate(symbol, {
            val offset = ramOffset
            ramOffset += ramDepth(symbol.valueType)
            offset
        })
    }

    private def guard(state: Int): String = {
        val gl = guards.getOrElse(state, new ArrayBuffer[String]).toSet
        if (gl.isEmpty) {
            return "1"
        } else {
            gl.mkString(" & ")
        }
    }

    def addReadState(state: Int, port: String) {
        val a = readStates.getOrElseUpdate(port, { new Assignment(port) })
        a.states += new AssignState(state, null)
        addGuard(state, "avail_" + port)
    }

    def addWriteState(state: Int, port: String, value: String) {
        val a = writeStates.getOrElseUpdate(port, { new Assignment(port) })
        a.states += new AssignState(state, value)
        addGuard(state, "!afull_" + port)
    }

    def addAssignment(str: String) {
        assignments += str
    }

    def addPhi(phi: IRPhi) {
        phis += phi
    }

    def addState(state: Int) {
        guards.getOrElseUpdate(state, { new ArrayBuffer[String] })
    }

    private def emitComponent(component: Component) {

        val parts = component.parts
        val width = component.width
        val name = component.name
        val instanceName = component.instanceName

        write("reg " + instanceName + "_start;")
        for (i <- 0 until component.argCount) {
            write("reg [" + (width - 1) + ":0] " + instanceName + "_" + i + ";")
        }
        write("wire [" + (width - 1) + ":0] " + instanceName + "_result;")
        write("wire " + instanceName + "_ready;")

        write(name + " #(.WIDTH(" + width + "))")
        enter
        write(instanceName + "(clk,")
        enter
        write(instanceName + "_start,")
        for (i <- 0 until component.argCount) {
            write(instanceName + "_" + i + ", ")
        }
        write(instanceName + "_result, " + instanceName + "_ready);")
        leave
        leave
        write

        emitMux(component)

    }

    private def emitMux(component: Component) {

        val parts = component.parts.toSeq
        val sortedParts = parts.sortWith { (a, b) => a.state > b.state }
        val width = component.width
        val instanceName = component.instanceName

        write
        write("always @(*) begin")
        enter

        write(s"${instanceName}_start <= 0;")
        sortedParts.foreach { p =>
            val state = p.state
            write(s"if (state == $state) begin")
            enter
            write(s"${instanceName}_start <= last_state != state;")
            p.args.zipWithIndex.foreach { case (s, i) =>
                write(s"${instanceName}_$i <= $s;")
            }
            leave
            write("end")
        }

        leave
        write("end")
        write

    }

    private def emitParts {
        components.values.foreach(emitComponent)
    }

    private def emitSimpleComponents {
        simpleComponents.values.foreach { c =>
            val args = c.args.mkString(", ")
            write(s"wire [${c.width - 1}:0] ${c.instanceName}_result;");
            write(s"${c.name} #(.WIDTH(${c.width}))")
            enter
            write(s"${c.instanceName}($args, ${c.instanceName}_result);")
            leave
            write
        }
    }

    private def getCondition[A <: StateCondition](lst: Traversable[A]) = {
        val strs = lst.map { s =>
            val state = s.state
            s"((state == $state) & guard_$state)"
        }
        strs.mkString(" | ")
    }

    private def emitAssignments {

        def select(lst: Traversable[AssignState]) = lst.foldLeft("") { (a, s) =>
            val state = s.state
            val value = s.value
            if (a.isEmpty) value.toString
            else s" state == $state ? $value : ($a)"
        }

        readStates.values.foreach { s =>
            val port = s.port
            val cond = getCondition(s.states)
            write(s"assign read_$port = $cond;")
        }
        writeStates.values.foreach { s =>
            val port = s.port
            val cond = getCondition(s.states)
            val value = select(s.states)
            write(s"assign write_$port = $cond;")
            write(s"assign output_$port = $value;")
        }

        if (!assignments.isEmpty) {

            // We place a dummy register in the always to work around
            // an issue where iverilog fails to trigger if there are
            // only constants on the right-hand side.
            write("reg dummy;")
            write("always @(*) begin")
            enter
            write("dummy <= rst;")
            assignments.foreach(write)
            leave
            write("end")
        }

    }

    private def emitPhis {

        phis.foreach { phi =>
            write("always @(*) begin")
            enter
            write("case (prev_state)")
            enter
            val dest = emitSymbol(phi.dest)
            phi.inputs.tail.foreach { case (block, symbol) =>
                val state = getNextState(graph, block)
                val sym = emitSymbol(symbol)
                write(s"$state: $dest <= $sym;")
            }
            write(s"default: $dest <= " +
                  emitSymbol(phi.inputs.values.head) + ";")
            leave
            write("endcase")
            leave
            write("end")
            write
        }

    }

    private def emitGuards {
        guards.keys.foreach { s =>
            val cond = guard(s)
            write(s"wire guard_$s = $cond;")
        }
    }

    override def getOutput(): String = {
        enter
        emitParts
        emitSimpleComponents
        emitGuards
        emitAssignments
        emitPhis
        leave
        super.getOutput()
    }

}
