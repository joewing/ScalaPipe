package scalapipe.kernels

import scalapipe.dsl._

object MT19937State extends Kernel {

    val state = output(UNSIGNED32)
    val seed = config(UNSIGNED32, 'seed, 15)

    val last = local(UNSIGNED32, 'seed)
    val index = local(UNSIGNED32, 0)

    if (index < 624) {
        state = last
        last = 0x6c078965 * (last ^ (last >> 30)) + index
        index += 1
    } else {
        stop
    }

}
