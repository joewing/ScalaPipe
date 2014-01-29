package scalapipe.kernels

import scalapipe.dsl._

class InputSocket(t: AutoPipeType, port: Int) extends Kernel {

    val out     = output(t)
    val buffer  = local(t)
    val sock    = local(SIGNED32, -1)

    if (sock < 0) {
        sock = socket.ap_connect(port)
        if (sock < 0) {
            stdio.exit(-1)
        }
    }

    if (socket.recv(sock, addr(buffer), sizeof(t), 0) > 0) {
        out = buffer
    }

}
