
package autopipe.gen

import autopipe._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

private[gen] class HDLModuleEmitter(
        val co: CodeObject,
        val graph: IRGraph
    ) extends HDLGenerator {

    private class Part(val state: Int, val args: List[String])

    private class Component(val name: String,
                                    val instanceName: String,
                                    val argCount: Int,
                                    val width: Int) {
        val parts = new ListBuffer[Part]
    }

    private class SimpleComponent(val name: String,
                                            val instanceName: String,
                                            val args: List[String],
                                            val width: Int)

    private class StateCondition(val state: Int)

    private class AssignState(_state: Int, val value: String)
        extends StateCondition(_state)

    private class Assignment(val port: String) {
        val states = new ListBuffer[AssignState]
    }

    private class RAMState(_state: Int, val value: String, val offset: String)
        extends StateCondition(_state)
        
    private class RAMUpdate(val port: String) {
        val states = new ListBuffer[RAMState]
    }

    private val share = co.parameters.get[Int]('share)
    private val components = new HashMap[String, Component]
    private val simpleComponents = new HashMap[String, SimpleComponent]
    private val componentIds = new HashMap[String, Int]
    private val readStates = new HashMap[String, Assignment]
    private val writeStates = new HashMap[String, Assignment]
    private val ramWrites = new HashMap[String, RAMUpdate]
    private val ramReads = new HashMap[String, RAMUpdate]
    private val assignments = new ListBuffer[String]
    private val phis = new ListBuffer[IRPhi]
    private val guards = new HashMap[Int, ArrayBuffer[String]]

    def create(name: String, width: Int, state: Int,
                  args: List[String]): String = {
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

    def createSimple(name: String, width: Int, args: List[String]): String = {
        val i = componentIds.getOrElseUpdate(name, 0)
        componentIds.update(name, i + 1)
        val instanceName = name + "x" + i.toString
        val comp = simpleComponents.getOrElseUpdate(instanceName, {
            new SimpleComponent(name, instanceName, args, width)
        })
        return instanceName + "_result"
    }

    private def addGuard(state: Int, guard: String) {
        val gl = guards.getOrElseUpdate(state, { new ArrayBuffer[String] })
        gl += guard
    }

    private def guard(state: Int): String = {
        val gl = guards.getOrElse(state, new ArrayBuffer[String]).sorted.distinct
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

    def addRAMWrite(state: Int, port: String, src: String, offset: String) {
        val a = ramWrites.getOrElseUpdate(port, { new RAMUpdate(port) })
        a.states += new RAMState(state, src, offset)
    }

    def addRAMRead(state: Int, port: String, dest: String, offset: String) {
        val a = ramReads.getOrElseUpdate(port, { new RAMUpdate(port) })
        a.states += new RAMState(state, dest, offset)
        addGuard(state, "(last_state == state)")
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

        val parts = component.parts
        val sortedParts = parts.sortWith { (a, b) => a.state > b.state }
        val width = component.width
        val instanceName = component.instanceName

        write("always @(*) begin")
        enter

        write(instanceName + "_start <= 0;")
        sortedParts.foreach { p =>
            write ("if (state == " + p.state + ") begin")
            enter
            write(instanceName + "_start <= last_state != state;")
            p.args.zipWithIndex.foreach { case (s, i) =>
                write(instanceName + "_" + i + " <= " + s + ";")
            }
            leave
            write("end")
        }

        leave
        write("end")
        write

    }

    private def emitParts {
        components.values.foreach { c => emitComponent(c) }
    }

    private def emitSimpleComponents {
        simpleComponents.values.foreach { c =>
            set("width", c.width)
            set("name", c.name)
            set("instanceName", c.instanceName)
            set("args", c.args.mkString(", "))
            write("wire [$width - 1$:0] $instanceName$_result;");
            write("$name$ #(.WIDTH($width$))")
            enter
            write("$instanceName$($args$, $instanceName$_result);")
            leave
            write
        }
    }

    private def getCondition[A <: StateCondition](lst: ListBuffer[A]): String = {
        lst.foldLeft("") { (a, b) =>
            a + (if (a.isEmpty) "" else " | ") +
            "((state == " + b.state + ") & guard_" + b.state + ")"
        }
    }

    private def emitAssignments {

        def select(lst: List[AssignState]): String = lst match {
            case h :: Nil => h.value
            case h :: t =>
                " state == " + h.state + " ? " + h.value + " : (" + select(t) + ")"
            case Nil => ""
        }

        readStates.values.foreach { s =>
            write("assign read_" + s.port + " = " + getCondition(s.states) + ";")
        }
        writeStates.values.foreach { s =>
            write("assign write_" + s.port + " = " + getCondition(s.states) + ";")
            write("assign output_" + s.port + " = " +
                    select(s.states.toList) + ";")
        }
        write

        if (!assignments.isEmpty) {
            write("always @(*) begin")
            enter
            assignments.foreach { s => write(s) }
            leave
            write("end")
            write
        }

    }

    private def emitRAMUpdates {

        def value(lst: List[RAMState]): String = lst match {
            case h :: Nil => h.value
            case h :: t =>
                " state == " + h.state + " ? " + h.value + " : (" + value(t) + ")"
            case Nil => ""
        }

        def index(lst: List[RAMState]): String = lst match {
            case h :: Nil => h.offset
            case h :: t =>
                " state == " + h.state + " ? " + h.offset + " : (" + index(t) + ")"
            case Nil => ""
        }

        ramWrites.values.foreach { s =>
            write("assign " + s.port + "_we = " + getCondition(s.states) + ";")
            write("assign " + s.port + "_in = " + value(s.states.toList) + ";")
            write("assign " + s.port + "_wix = " + index(s.states.toList) + ";")
        }

        ramReads.values.foreach { s =>
            write("assign " + s.port + "_rix = " + index(s.states.toList) + ";")
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
                write(state + ": " + dest + " <= " + emitSymbol(symbol) + ";")
            }
            write("default: " + dest + " <= " +
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
            write("wire guard_" + s + " = " + guard(s) + ";")
        }
        write
    }

    override def getOutput(): String = {
        enter
        emitParts
        emitSimpleComponents
        emitGuards
        emitAssignments
        emitRAMUpdates
        emitPhis
        leave
        super.getOutput()
    }

}

