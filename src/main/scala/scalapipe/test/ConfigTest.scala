package scalapipe.test

import scalapipe.dsl._
import scalapipe.kernels._

object ConfigTest extends App { 

    val Gen = new Kernel("Gen") {
        val y0 = output(UNSIGNED32)
        val count = local(UNSIGNED32, 0)
        val max_count = config(UNSIGNED32, 'max_count, 0)

        y0 = count
        count += 1
        if (count == max_count) {
            stop
        }
    }

    val Print = new Kernel("Print") {
        val x0 = input(UNSIGNED32)
        stdio.printf("OUTPUT %u\n", x0)
    }

    val app = new Application {
        val mc = config('mc, 2)
        val m2 = config('m2, mc)
        Print(Gen('max_count -> m2))
    }
    app.emit("ConfigTest")

}
