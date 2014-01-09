
package autopipe.gen

import autopipe._

private[autopipe] class SockEdgeGenerator
    extends EdgeGenerator(Platforms.C) {

    override def emitCommon() {

        write("#include <sys/types.h>")
        write("#include <sys/socket.h>")
        write("#include <netinet/in.h>")
        write("#include <netinet/tcp.h>")
        write("#include <arpa/inet.h>")
        write("#include <sys/ioctl.h>")
        write("#include <netdb.h>")
        write("#include <fcntl.h>")

    }

    override def emitGlobals(streams: Traversable[Stream]) {

        for (stream <- streams) {
        }

    }

}

