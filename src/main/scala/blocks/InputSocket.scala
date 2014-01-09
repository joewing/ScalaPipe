
package blocks

import autopipe.dsl._

class InputSocket(t: AutoPipeType, port: Int) extends AutoPipeBlock {

    val out      = output(t)

    val CHARPTR = new AutoPipePointer(SIGNED8)

    val buffer  = local(t)
    val sock     = local(SIGNED32, -1)

    if (sock < 0) {
        sock = socket.ap_connect(port)
        if (sock < 0) {
            stdio.exit(-1)
        }
    }

    // Read input.
    if (socket.recv(sock, addr(buffer), sizeof(t), 0) > 0) {
        out = buffer
    }

}

