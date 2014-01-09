
package blocks

import autopipe.dsl._

class AverageBlock(t: AutoPipeType) extends AutoPipeBlock {

    val x0 = input(t)
    val x1 = input(t)
    val y0 = output(t)

    y0 = (x0 + x1) / 2

/*
    new AutoPipeBlockTest(this) {
        input(0, 5)
        input(1, 15)
        output(0, 10)
        run
    }
*/

}

