package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object FunctionTest {

    val Func1 = new Func("Func1") {
        val x0 = input(SIGNED32)
        val x1 = input(SIGNED32)
        return x0 + x1
    }

    val Block1 = new Kernel("Block1") {
        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        y0 = Func1(x0, 1)
    }

    val Generate = new Kernel("Generate") {
        val y0 = output(SIGNED32)
        val temp = local(SIGNED32, 0)
        y0 = temp
        temp += 1
        if (temp == 10) {
            stop
        }
    }

    val Print = new Kernel("Print") {
        val x0 = input(SIGNED32)
        val t = local(SIGNED32, 0)
        stdio.printf("OUTPUT %d\n", x0)
        t += 1
        if (t == 10) stdio.exit(0)
    }

    def main(args: Array[String]) {

        val mapping = if (args.length > 0) args(0).toInt else 0
        val app = new Application {
            val s1 = Generate()
            val s2 = Generate()
            val total = Func1(s1, s2)
            val result = Block1(total)
            Print(result)

            mapping match {
                case 0 => ()
                case 1 =>
                    map(ANY_KERNEL -> Block1, CPU2FPGA())
                    map(ANY_KERNEL -> Print, FPGA2CPU())
                case 2 =>
                    map(ANY_KERNEL -> Print, FPGA2CPU())
                case 3 =>
                    map(ANY_KERNEL -> Func1, CPU2FPGA())
                    map(Func1 -> ANY_KERNEL, FPGA2CPU())
            }

        }
        app.emit("FunctionTest")

    }

}
