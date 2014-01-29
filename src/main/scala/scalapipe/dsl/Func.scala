package scalapipe.dsl

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import scalapipe._

class Func(_name: String) extends Kernel(_name) {

    def this() = this(LabelMaker.getKernelLabel)

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

    // Return type.
    def returns(t: AutoPipeType) {
        output(t)
    }

    // Return a value.
    // FIXME: It would be nice to be able to override __return instead.
    def ret(result: ASTNode = null) {
        ASTReturnNode(result, this)
    }

}
