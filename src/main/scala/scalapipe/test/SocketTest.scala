package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object SocketTest extends App {

    val Gen = new Kernel("Gen") {
        val y0 = output(UNSIGNED32)
        for (i <- 0 until 10) {
            y0 = i
        }
        stop
    }

    val Print = new Kernel("Print") {
        val x0 = input(UNSIGNED32)
        stdio.printf("OUTPUT %d\n", x0)
    }

    val app = new Application {
        Print(Gen())
        map(Gen -> Print, CPU2CPU(host = "127.0.0.1"))
    }
    app.emit("SocketTest")

}
