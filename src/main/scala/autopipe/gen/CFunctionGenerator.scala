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

        val prof = optional(ft.parameters.get('profile), "unsigned long*")
        val args = prof.toSeq ++ ft.inputs.map(_.valueType.toString)
        val argString = args.mkString(", ")
        val fname = ft.name
        val rtype = ft.returnType
        write(s"$rtype $fname($argString);")

    }

    protected override def emitFunctionSource {

        val timing: Map[ASTNode, Int] =
            if (ft.expression.pure && ft.parameters.get('profile)) {
                val ir              = IRNodeEmitter(ft).emit(ft.expression)
                val context         = new ProfileIRContext(ft)
                val graph           = IROptimizer(ft, context).optimize(ir)
                val moduleEmitter   = new HDLModuleEmitter(ft, graph)
                HDLTiming.computeAST(graph)
            } else null

        val prof = optional(ft.parameters.get('profile),
                            "unsigned long *clocks")
        val args = prof.toSeq ++ ft.inputs.map { a =>
            a.valueType.toString + " " + a.name
        }
        val argString = args.mkString(", ")
        val rtype = ft.returnType
        val fname = ft.name
        write(s"$rtype $fname($argString)")
        enter
        for ((name, vtype) <- ft.states.map(l => (l.name, l.valueType))) {
            write(s"$vtype $name;")
        }
        val nodeEmitter = new CFunctionNodeEmitter(ft, timing)
        nodeEmitter.emit(ft.expression)
        write(nodeEmitter)
        leave

    }

}
