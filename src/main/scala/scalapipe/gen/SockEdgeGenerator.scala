package scalapipe.gen

import scalapipe._

private[scalapipe] class SockEdgeGenerator
    extends EdgeGenerator(Platforms.C) with CGenerator {

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
        streams.foreach { s => writeGlobals(s) }
    }

    override def emitInit(streams: Traversable[Stream]) {
        streams.foreach { s => writeInit(s) }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        streams.foreach { s => writeDestroy(s) }
    }

    private def writeGlobals(stream: Stream) {

        set("stream", stream.label)

        // "process" - Run on the consumer side (dest).
        write("static bool $stream$_process()")
        enter()
        leave()

    }

    private def writeInit(stream: Stream) {
    }

    private def writeDestroy(stream: Stream) {
    }

}
