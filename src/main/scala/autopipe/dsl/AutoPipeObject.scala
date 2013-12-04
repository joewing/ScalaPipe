
package autopipe.dsl

import autopipe._
import autopipe.gen.ObjectGenerator

abstract class AutoPipeObject(val name: String) {

   def this() = this(LabelMaker.getBlockLabel)

   private[autopipe] def generator(co: CodeObject): ObjectGenerator

   private[autopipe] def run(i: BlockInterface,
                             method: String,
                             args: Seq[ASTNode]): Literal = {
      sys.error("not implemented")
   }

}

