
package autopipe.dsl

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import autopipe._

trait ReturnType {

   private[autopipe] var returnType = ValueType.void

   def returns(t: AutoPipeType) {
      returnType = t.create()
   }

}

class AutoPipeFunction(_name: String)
   extends AutoPipeBlock(_name) with ReturnType {

   def this() = this(LabelMaker.getFunctionLabel)

   // Include paths needed for this function.
   def ipath(n: String) {
      dependencies.add(DependencySet.IPath, n)
   }

   // Library paths needed for this function.
   def lpath(n: String) {
      dependencies.add(DependencySet.LPath, n)
   }

   // Libraries needed for this function.
   def library(n: String) {
      dependencies.add(DependencySet.Library, n)
   }

   // Include files needed for this function.
   def include(n: String) {
      dependencies.add(DependencySet.Include, n)
   }

   // Arguments to this function.
   def argument(t: AutoPipeType) {
      input(t)
   }

   // Return a value.
   // FIXME: It would be nice to be able to override __return instead.
   def ret(result: ASTNode = null) {
      ASTReturnNode(result, this)
   }

}

