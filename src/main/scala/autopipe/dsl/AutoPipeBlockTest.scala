
package autopipe.dsl

import language.implicitConversions
import autopipe._
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue

class AutoPipeBlockTest(val apb: AutoPipeBlock) {

   private val blockType = new InternalBlockType(null, apb, Platforms.C)
   private val states = new HashMap[String, Literal]
   private val inputs = new HashMap[String, Queue[Literal]]
   private val outputs = new HashMap[String, Queue[Literal]]

   for (s <- blockType.states) {
      states(s.name) = s.value
   }

   private def inputQueue(symbol: String): Queue[Literal] = {
      inputs.getOrElseUpdate(symbol, new Queue[Literal])
   }

   private def outputQueue(symbol: String): Queue[Literal] = {
      outputs.getOrElseUpdate(symbol, new Queue[Literal])
   }

   private class StopException extends Exception

   private class InvalidException(val msg: String = "") extends Exception {
      override def toString(): String = msg
   }

   private class TestInterface extends BlockInterface {

      def available(symbol: String): Literal = {
         if (blockType.isInput(symbol)) {
            if (inputQueue(symbol).isEmpty) {
               new IntLiteral(ValueType.bool, 0)
            } else {
               new IntLiteral(ValueType.bool, 1)
            }
         } else if (blockType.isOutput(symbol)) {
            new IntLiteral(ValueType.bool, 1)
         } else {
            new IntLiteral(ValueType.bool, 1)
         }
      }

      def read(symbol: String, index: Literal): Literal = {
         if (blockType.isInput(symbol)) {
            val queue = inputQueue(symbol)
            if (queue.isEmpty) {
               throw new StopException()
            }
            val l = queue.dequeue()
            l(index)
         } else if (blockType.isState(symbol)) {
            states.get(symbol) match {
               case Some(l)   => l(index)
               case None      =>
                  throw new InvalidException("symbol " + symbol + " not found")
            }
         } else {
            throw new InvalidException("Invalid read")
         }
      }

      def write(symbol: String, index: Literal, value: Literal) {
println("WRITE: " + symbol + " = " + value)
         if (blockType.isOutput(symbol)) {
            val queue = outputQueue(symbol)
            if (!queue.isEmpty) {
               val l = queue.dequeue()
               if (!l.equals(value)) {
                  throw new InvalidException("Output mismatch: " +
                     "got " + value + " expected " + l)
               }
            }
         } else if (blockType.isState(symbol)) {
            states.get(symbol) match {
               case Some(l)   => states(symbol) = value
               case None      => states(symbol) = value
            }
         } else {
            throw new InvalidException("Invalid write")
         }
      }

      def call(symbol: String, args: Seq[Literal]): Literal = {
         throw new InvalidException("Call not implemented")
      }

      def stop() {
         throw new StopException()
      }

   }

   implicit def bool(b: Boolean): Literal = Literal.get(b)

   implicit def int(i: Int): Literal = Literal.get(i)

   implicit def long(l: Long): Literal = Literal.get(l)

   def input(port: Int, value: Literal) {
      val queue = inputQueue(blockType.inputName(port))
      queue += value
   }

   def output(port: Int, value: Literal) {
      val queue = outputQueue(blockType.outputName(port))
      queue += value
   }

   def run(): Boolean = {

      val interface = new TestInterface()

      try {
         while (true) {
            blockType.run(interface)
         }
      } catch {
         case stop: StopException =>
            println("stop")
         case inv:  InvalidException =>
            println("error: " + inv.toString)
            return false
      }

      val inputsLeft = inputs.exists { a => !a._2.isEmpty }
      val outputsLeft = outputs.exists { a => !a._2.isEmpty }
      if (inputsLeft) {
         println("Not all input was consumed")
         false
      } else if (outputsLeft) {
         println("Not all output was consumed")
         false
      } else {
         println("pass")
         true
      }

   }

}

