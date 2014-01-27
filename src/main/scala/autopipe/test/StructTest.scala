package autopipe.test

import blocks._
import autopipe.dsl._

object StructTest {

    val Struct1 = new AutoPipeStruct {
        field('a, SIGNED32)
        field('b, BOOL)
    }

    val Struct2 = new AutoPipeStruct {
        field('x, Struct1)
        field('y, UNSIGNED32)
    }

    val Gen = new AutoPipeFunction("Gen") {
        val y0      = output(Struct2)
        val count   = local(UNSIGNED32, 0)
        val t       = local(Struct2)

        t('x)('a) = count + 1
        t('x)('b) = count & 1
        t('y) = count
        y0 = t
        count += 1
    }

    val Print = new AutoPipeBlock("Print") {
        val x0  = input(Struct2)
        val t   = local(Struct2)

        t = x0
        stdio.printf("""OUTPUT %d\n""", t('y))
        if (t('x)('a) <> t('y) + 1) {
            stdio.printf("""OUTPUT: ERROR a\n""")
        }
        if (t('x)('b) <> (t('y) & 1)) {
            stdio.printf("""OUTPUT: ERROR b\n""")
        }
        if (t('y) == 10) {
            stdio.exit(0)
        }
    }

    def main(args: Array[String]) {
        val app = new AutoPipeApp {
            Print(Gen())
        }
        app.emit("StructTest")
    }

}
