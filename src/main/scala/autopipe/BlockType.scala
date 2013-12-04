
package autopipe

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import autopipe.dsl._

private[autopipe] abstract class BlockType(_ap: AutoPipe,
                                           _name: String,
                                           _symbols: SymbolTable,
                                           _platform: Platforms.Value,
                                           _loopBack: Boolean)
      extends CodeObject(_ap, _name, _symbols, _platform, _loopBack) {

   private[autopipe] val label = LabelMaker.getTypeLabel
   private[autopipe] val inputs = symbols.inputs
   private[autopipe] val outputs = symbols.outputs
   private[autopipe] var blocks = new ListBuffer[Block]

   def this(ap: AutoPipe, apb: AutoPipeBlock, p: Platforms.Value) = {
      this(ap, apb.name, new SymbolTable(apb), p, apb.loopBack)
      apb.inputs.foreach(i => symbols.addInput(i.name, i.t.create()))
      apb match {
         case apf: AutoPipeFunction if apf.returnType != ValueType.void =>
            symbols.addOutput("output", apf.returnType)
         case _ =>
            apb.outputs.foreach(o => symbols.addOutput(o.name, o.t.create()))
      }
      apb.configs.foreach { c =>
         symbols.addConfig(c.name, c.t.create(), Literal.get(c.default, apb))
      }
      apb.states.foreach { s =>
         symbols.addState(s.name, s.t, Literal.get(s.init, apb))
      }
      dependencies.add(apb.dependencies)
   }

   private[autopipe] def addBlock(b: Block) {
      blocks += b
   }

   private[autopipe] def inputType(p: PortName): ValueType = {
      val s = symbols.getInput(p)
      if (s == null) {
         Error.raise("input port " + p + " not found", this)
      }
      s.valueType
   }

   private[autopipe] def outputType(p: PortName): ValueType = {
      val s = symbols.getOutput(p)
      if (s == null) {
         Error.raise("output port " + p + " not found", this)
      }
      s.valueType
   }

   private[autopipe] def inputIndex(p: PortName): Int = symbols.inputIndex(p)

   private[autopipe] def outputIndex(p: PortName): Int = symbols.outputIndex(p)

   def inputIndex(n: String): Int = inputIndex(new StringPortName(n))

   def outputIndex(n: String): Int = outputIndex(new StringPortName(n))

   private[autopipe] def emit(dir: java.io.File)

   private[autopipe] def run(i: BlockInterface)

   private[autopipe] def internal: Boolean

   private[autopipe] def inputName(i: Int) = inputs(i).name

   private[autopipe] def outputName(i: Int) = outputs(i).name

   private[autopipe] def streams = blocks.flatMap(b => b.getInputs)

   def isInput(n: String) = inputs.exists(_.name == n)

   def isOutput(n: String) = outputs.exists(_.name == n)

   def isPort(n: String) = isInput(n) || isOutput(n)

   def isState(n: String) = states.exists(_.name == n)

   def isConfig(n: String) = configs.exists(_.name == n)

   def isLocal(n: String) = symbols.get(n) match {
      case sn: StateSymbol => sn.isLocal
      case _               => false
   }

}

