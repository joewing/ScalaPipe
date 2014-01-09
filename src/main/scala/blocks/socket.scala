
package blocks

import autopipe.dsl._

object socket {

    class sockFunc(_name: String) extends AutoPipeFunction(_name) {
        include("sys/types.h")
        include("sys/socket.h")
        include("unistd.h")
        external("C")
    }

    val ap_connect = new sockFunc("ap_connect") {
        returns(SIGNED32)
    }

    val ap_accept = new sockFunc("ap_accept") {
        returns(SIGNED32)
    }

    val send = new sockFunc("send") {
        returns(SIGNED64)
    }

    val recv = new sockFunc("recv") {
        returns(SIGNED64)
    }

    val close = new sockFunc("close") {
        returns(SIGNED32)
    }

}

