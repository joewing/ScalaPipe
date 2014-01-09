
package autopipe.dsl

import autopipe._
import autopipe.gen.ObjectGenerator

class ROM[T](val valueType: AutoPipeType, val data: Seq[T])
        extends AutoPipeObject {

    def get(i: ASTNode)(implicit block: AutoPipeBlock): ASTNode = {
        val node = ASTSpecial(this, "get", block)
        node.args = List(i)
        node.valueType = valueType.create
        node
    }

    private abstract class ROMObjectGenerator(_co: CodeObject)
        extends ObjectGenerator(_co) {
    }

    private class HDLROM(_co: CodeObject) extends ROMObjectGenerator(_co) {

        override def emitModule: String = {
            getOutput
        }

        override def emitCall(op: String, args: Seq[BaseSymbol]): String = {
            ""
        }

    }

    private class CROM(_co: CodeObject) extends ROMObjectGenerator(_co) {

        override def emitModule: String = {
            write(valueType.name + " " + co.name + "_get(int i) {")
            enter
            write("switch(i) {")
            data.zipWithIndex.foreach { case (v, i) =>
                write("case " + i + ": return " + v + ";")
            }
            write("default: return 0;")
            write("}")
            leave
            write("}")
            write
            getOutput
        }

        override def emitCall(op: String, args: Seq[BaseSymbol]): String = {
            val index = args.head
            assert(op == "get")
            co.name + "_get(block->" + index + ")"
        }

    }

    private[autopipe] def generator(co: CodeObject): ObjectGenerator = {
        co.platform match {
            case Platforms.HDL    => new HDLROM(co)
            case Platforms.C      => new CROM(co)
            case _ => Error.raise("ROM not implemented on " + co.platform)
        }
    } 

}

