
package autopipe

import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import autopipe.dsl.AutoPipeBlock

private[autopipe] class SymbolTable(apb: AutoPipeBlock) {

   private val symbols = new HashMap[String, BaseSymbol]
   private[autopipe] val inputs = new ListBuffer[InputSymbol]
   private[autopipe] val outputs = new ListBuffer[OutputSymbol]
   private[autopipe] val configs = new ListBuffer[ConfigSymbol]
   private[autopipe] val states = new ListBuffer[StateSymbol]
   private[autopipe] val temps = new ListBuffer[TempSymbol]
   private[autopipe] val freeTemps = new ListBuffer[TempSymbol]

   private def add(n: String, s: BaseSymbol) {
      if (symbols.contains(n)) {
         Error.raise("duplicate symbol: " + n, apb)
      } else {
         symbols += ((n, s))
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

   def addConfig(name: String, vt: ValueType, value: Literal) {
      val s = new ConfigSymbol(name, vt, value)
      add(s)
      configs += s
   }

   def addState(name: String, vt: ValueType, value: Literal) {
      val s = new StateSymbol(name, vt, value)
      add(s)
      states += s
   }

   def createTemp(vt: ValueType): TempSymbol = {
      val tl = freeTemps.filter { t => t.valueType == vt }
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
      case Some(s)   => s
      case None      => null
   }

   def getType(name: String): ValueType = symbols.get(name) match {
      case Some(s)   => s.valueType
      case None      => null
   }

   def getInput(pn: PortName): InputSymbol = {
      if (pn.isIndex) {
         if (pn.index >= 0 && pn.index < inputs.size) {
            inputs(pn.index)
         } else {
            null
         }
      } else {
         symbols.get(pn.toString) match {
            case Some(s) if s.isInstanceOf[InputSymbol] =>
               s.asInstanceOf[InputSymbol]
            case _ => null
         }
      }
   }

   def getOutput(pn: PortName): OutputSymbol = {
      if (pn.isIndex) {
         if (pn.index >= 0 && pn.index < outputs.size) {
            outputs(pn.index)
         } else {
            null
         }
      } else {
         symbols.get(pn.toString) match {
            case Some(s) if s.isInstanceOf[OutputSymbol] =>
               s.asInstanceOf[OutputSymbol]
            case _ => null
         }
      }
   }

   def inputIndex(pn: PortName): Int = {
      if (pn.isIndex) {
         pn.index
      } else {
         inputs.indexOf(getInput(pn))
      }
   }

   def outputIndex(pn: PortName): Int = {
      if (pn.isIndex) {
         pn.index
      } else {
         outputs.indexOf(getOutput(pn))
      }
   }

   def getBaseOffset(s: String): Int = {
      var result = 0
      for (s <- states.takeWhile(_ != s)) {
         s.valueType match {
            case at: ArrayValueType => result += (at.bits + 7) / 8
            case _                  => result += 0
         }
      }
      return result
   }

}

