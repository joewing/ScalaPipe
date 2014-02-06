package scalapipe.test

import scalapipe.kernels._
import scalapipe.dsl._

object FunctionTest {

    val Add = new Func("Add") {
        val x0 = input(SIGNED32)
        val x1 = input(SIGNED32)
        return x0 + x1
    }

    val Increment = new Kernel("Increment") {
        val x0 = input(SIGNED32)
        val y0 = output(SIGNED32)
        y0 = Add(x0, 1)
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

        val mapping = args.headOption.getOrElse("0").toInt
        val app = new Application {
            val s1 = Generate()
            val s2 = Generate()
            val total = Add(s1, s2)
            val result = Increment(total)
            Print(result)

            mapping match {
                case 0 => ()
                case 1 =>
                    map(ANY_KERNEL -> Increment, CPU2FPGA())
                    map(ANY_KERNEL -> Print, FPGA2CPU())
                case 2 =>
                    map(ANY_KERNEL -> Print, FPGA2CPU())
                case 3 =>
                    map(ANY_KERNEL -> Add, CPU2FPGA())
                    map(Add -> ANY_KERNEL, FPGA2CPU())
            }

        }
        app.emit("FunctionTest")

    }

}
