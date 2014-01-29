package scalapipe.kernels

import scalapipe.dsl._

class OutputSocket(t: Type, port: Int) extends Kernel {

    val in      = input(t)
    val buffer  = local(t)
    val sock    = local(SIGNED32, -1)

    if (sock < 0) {
        sock = socket.ap_connect(port)
        if (sock < 0) {
            stdio.exit(-1)
        }
    }

    buffer = in
    if (socket.send(sock, addr(buffer), sizeof(buffer), 0) < 0) {
        stdio.exit(-1)
    }

}
