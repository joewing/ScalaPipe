package scalapipe.gen

import scalapipe._

private[scalapipe] class SockEdgeGenerator(val host: String)
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
        streams.foreach { s =>
            if (s.sourceKernel.device.host == host) {
                writeProducerGlobals(s)
            } else {
                writeConsumerGlobals(s)
            }
        }
    }

    override def emitInit(streams: Traversable[Stream]) {
        streams.foreach { s => writeInit(s) }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        streams.foreach { s => writeDestroy(s) }
    }

    private def writeProducerGlobals(stream: Stream) {

        val sock = s"sock${stream.label}"
        val bufname = s"buffer${stream.label}"
        val vtype = stream.valueType

        // "get_free"
        write(s"static bool ${stream.label}_get_free()")
        enter
        write(s"static int size = 0;")
        writeIf(s"XUNLIKELY(size == 0)")
        write(s"socklen_t olen = sizeof(size);")
        write(s"getsockopt($sock, SOL_SOCKET, SO_SNDBUF, &size, &olen);")
        writeEnd
        write(s"int usage = 0;")
        write(s"ioctl($sock, TIOCOUTQ, &usage);")
        writeReturn(s"(size - usage) / sizeof($vtype)")
        leave

        // "allocate"
        write(s"static void *${stream.label}_allocate(int count)")
        enter
        write(s"static int size = 0;")
        write(s"const size_t temp = count * sizeof($vtype);")
        writeIf(s"XUNLIKELY(temp > size)")
        write(s"size = temp;")
        write(s"$bufname = realloc($bufname, size);")
        writeEnd
        writeReturn(bufname)
        leave

        // "send"
        write(s"static void ${stream.label}_send(int count)")
        enter
        write(s"const size_t size = count * sizeof($vtype);")
        write(s"const int c = send($sock, $bufname, size);")
        writeIf(s"XUNLIKELY(c < 0)")
        write("perror(\"send failed\");")
        write("exit(-1);")
        writeEnd
        leave

    }

    private def writeConsumerGlobals(stream: Stream) {

        val sock = s"sock${stream.label}"
        val lsock = s"server${stream.label}"
        val qname = s"q${stream.label}"
        val vtype = stream.valueType

        // "process"
        write(s"static bool ${stream.label}_process()")
        enter
        write(s"static ssize_t leftovers = 0;")
        writeIf(s"XUNLIKELY($sock == 0)")
        write(s"struct sockaddr_in caddr;")
        write(s"socklen_t caddrlen = sizeof(caddr);")
        write(s"$sock = accept($lsock, (struct sockaddr*)&caddr, &caddrlen);")
        writeIf(s"$sock < 0) {")
        writeIf(s"errno == EAGAIN")
        write(s"$sock = 0;")
        writeReturn("0")
        writeEnd
        write("perror(\"accept\");")
        write("exit(-1);")
        writeEnd
        write(s"fnctl($sock, F_SETFL, O_NONBLOCK);")
        writeEnd
        write(s"const size_t max_count = ${qname}->depth >> 3;")
        write(s"const size_t max_size = sizeof($vtype) * max_count")

        // TODO

        write(s"void *ptr = APQ_StartWrite($qname, max_count);")
        writeIf(s"ptr != NULL")
        write(s"ssize_t rc = recv($sock, ptr, max_size, 0);")
        writeIf(s"XUNLIKELY(rc < 0 && errno != EAGAIN)")
        write("perror(\"recv\");")
        write("exit(-1);")
        writeEnd
        writeEnd
        leave

        // "release"
        write(s"static void ${stream.label}_release(int count)")
        enter
        leave

        // "is_empty"
        write(s"static int ${stream.label}_is_empty()")
        enter
        writeReturn("0")
        leave

    }

    private def writeInit(stream: Stream) {
    }

    private def writeDestroy(stream: Stream) {
    }

}
