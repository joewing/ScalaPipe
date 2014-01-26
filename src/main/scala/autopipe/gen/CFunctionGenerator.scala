package autopipe.gen

import autopipe._
import autopipe.opt.IROptimizer
import java.io.File

private[autopipe] class CFunctionGenerator(
        val ft: InternalFunctionType
    ) extends CKernelGenerator(ft) {

    private def optional[T](cond: Boolean, value: T): Option[T] = {
        if (cond) Some(value) else None
    }

    protected override def emitFunctionHeader {

        val typeEmitter = new CTypeEmitter
        ft.states.foreach { s => typeEmitter.emit(s.valueType) }
        ft.inputs.foreach { i => typeEmitter.emit(i.valueType) }
        typeEmitter.emit(ft.returnType)
        write(typeEmitter)

        write
        val prof = optional(ft.parameters.get('profile), "unsigned long*")
        val args = prof.toList ++ ft.inputs.map(_.valueType.toString)
        val argString = args.mkString(", ")
        write(ft.returnType.toString + " " + ft.name + "(" + argString + ");")
        write

    }

    protected override def emitFunctionSource {

        val timing: Map[ASTNode, Int] =
            if (ft.expression.isPure && ft.parameters.get('profile)) {
                val ir              = IRNodeEmitter(ft).emit(ft.expression)
                val context         = new ProfileIRContext(ft)
                val graph           = IROptimizer(ft, context).optimize(ir)
                val moduleEmitter   = new HDLModuleEmitter(ft, graph)
                HDLTiming.computeAST(graph)
            } else null

        val prof = optional(ft.parameters.get('profile),
                            "unsigned long *clocks")
        val args = prof.toList ++ ft.inputs.map { a =>
            a.valueType.toString + " " + a.name
        }
        val argString = args.mkString(", ")
        write(ft.returnType.toString + " " + ft.name + "(" + argString + ")")
        write("{")
        enter

        // Declare locals.
        ft.states.foreach { l =>
            write(l.valueType + " " + l.name + ";")
        }
        write

        // Emit the code.
        val nodeEmitter = new CFunctionNodeEmitter(ft, timing)
        nodeEmitter.emit(ft.expression)
        write(nodeEmitter)
        write

        leave
        write("}")
        write

    }

}
