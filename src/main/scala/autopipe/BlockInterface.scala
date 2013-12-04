
package autopipe

trait BlockInterface {

   def available(symbol: String): Literal

   def read(symbol: String, index: Literal): Literal

   def write(symbol: String, index: Literal, value: Literal)

   def call(symbol: String, args: Seq[Literal]): Literal

   def stop()

}

