package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object ControlTest {

    val Gen = new Kernel {
        val y0 = output(SIGNED32)
        y0 = 5
        stop
    }

    // Test the "if" statement.
    val TestIf = new Kernel {
        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        val t = local(SIGNED32)

        t = x0

        // Else
        if (t < 5) {
            stop
        } else if (t > 5) {
            stop
        }

        // Else if
        if (t < 5) {
            stop
        } else if (t == 5) {
        } else {
            stop
        }

        // If
        if (t == 5) {
            y0 = t
        } else {
            stop
        }

    }

    // Test the "switch" statement.
    val TestSwitch = new Kernel {

        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        val t = local(SIGNED32)
        val ok = local(SIGNED32, 1)

        t = x0

        // When
        ok = 0
        switch(t) {
            when(0) {
                stop
            }
            when(5) {
                ok = 1
            }
        }
        if(!ok) {
            stop
        }

        // Others
        ok = 0
        switch(t) {
            when(4) {
                stop
            }
            others {
                ok = 1
            }
        }
        if(!ok) {
            stop
        }

        // Nothing
        switch(t) {
        }

        y0 = t

    }

    // Test "while"
    val TestWhile = new Kernel {
        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        val t = local(SIGNED32)
        val i = local(SIGNED32)

        t = x0

        // Zero iterations
        while (0) {
            stop
        }

        // t iterations.
        i = 0
        while (i < t) {
            i += 1
        }
        if (t <> i) {
            stop
        }

        y0 = t

    }

    // Test "for"
    val TestFor = new Kernel {
        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        val t = local(SIGNED32)
        val sum = local(SIGNED32)

        t = x0

        // Empty range 1.
        for (i <- 5 to 0) {
            stop
        }

        // Empty range 2.
        for (i <- 0 to 5 by -1) {
            stop
        }

        // Even values
        sum = 0
        for (i <- 0 to 10 by 2) {
            if (i & 1) {
                stop
            }
            sum += i
        }
        if (sum <> 2 + 4 + 6 + 8 + 10) {
            stop
        }

        // All values (exclusive).
        sum = 0
        for (i <- 0 until 10) {
            sum += i
        }
        if (sum <> 45) {
            stop
        }

        y0 = t

    }

    val Print = new Kernel {
        val x0 = input(SIGNED32)
        stdio.printf("OUTPUT %d\n", x0)
        stdio.exit(0)
    }

    def main(args: Array[String]) {

        val tests = Seq(TestIf, TestSwitch, TestWhile, TestFor)
        val mapping = if (args.length > 0) args(0).toInt else 0
        val app = new Application {
            val gen = Gen()
            val result = tests.foldLeft(gen) { (a, t) =>
                t(a)
            }
            Print(result)
            mapping match {
                case 0 => ()
                case 1 =>
                    map(Gen -> ANY_KERNEL, CPU2FPGA())
                    map(ANY_KERNEL -> Print, FPGA2CPU())
            }
        }
        app.emit("ControlTest")

    }

}
