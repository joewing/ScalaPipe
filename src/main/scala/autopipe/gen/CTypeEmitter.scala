
package autopipe.gen

import scala.collection.mutable.HashSet
import autopipe._

private[gen] trait CTypeEmitter extends Generator {

    private val emittedTypes = new HashSet[ValueType]

    def emitType(vt: ValueType) {
        if (!emittedTypes.contains(vt)) {
            emittedTypes += vt
            vt.dependencies.foreach { t => emitType(t) }
            vt match {
                case at: ArrayValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef struct {")
                    enter
                    write(at.itemType + " values[" + at.length + "];")
                    leave
                    write("} " + at.name + ";")
                    write("#endif")
                case td: TypeDefValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef " + td.value + " " + td.name + ";")
                    write("#endif")
                case pt: PointerValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef " + pt.itemType.name + " *" + pt.name + ";")
                    write("#endif")
                case ft: FixedValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef " + ft.baseType + " " + ft.name + ";")
                    write("#endif")
                case st: StructValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef struct {")
                    enter
                    for (f <- st.fields) {
                        val name = f._1
                        val tname = f._2.name
                        write(tname + " " + name)
                    }
                    leave
                    write("} " + st.name + ";")
                    write("#endif")
                case ut: UnionValueType =>
                    write("#ifndef DECLARED_" + vt.name)
                    write("#define DECLARED_" + vt.name)
                    write("typedef union {")
                    enter
                    for (f <- ut.fields) {
                        val name = f._1
                        val tname = f._2.name
                        write(tname + " " + name)
                    }
                    leave
                    write("} " + ut.name + ";")
                    write("#endif")
                case _ =>
                    write("// " + vt)
            }
        }
    }

}

