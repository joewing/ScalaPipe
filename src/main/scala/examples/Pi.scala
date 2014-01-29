package examples

import blocks._
import autopipe._
import autopipe.dsl._

object Pi extends App {

    val PiSim = new Kernel {

        val x0 = input(UNSIGNED32)
        val y0 = output(FLOAT32)

        val rx = local(UNSIGNED32)
        val ry = local(UNSIGNED32)
        val count = local(UNSIGNED32, 0)
        val hits = local(FLOAT32, 0)

        // Get the X and Y coordinates.
        rx = (x0 >>  0) & 0x7FFF
        ry = (x0 >> 16) & 0x7FFF

        // Determine if the dart hit the circle.
        count = count + 1
        if (rx * rx + ry * ry < (1 << 30)) {
            hits = hits + 1
        }

        // Output our current guess.
        y0 = (hits / count) * 4.0

    }

    val Print = new Kernel {
        val x0 = input(FLOAT32)
        stdio.printf("""%g\n""", x0)
    }

    val app = new AutoPipeApp {
        val state = GenState()
        val darts = MT19937(state)
        val result = PiSim(darts)
        Print(result)
    }
    app.emit("pi")

}
