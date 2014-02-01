package scalapipe.gen

import scalapipe._

private[scalapipe] class SockEdgeGenerator(
        val sp: ScalaPipe,
        val host: String
    ) extends EdgeGenerator(Platforms.C) with CGenerator {

    private def isProducer(s: Stream): Boolean = {
        s.sourceKernel.device.host == host
    }

    override def emitCommon() {
        write("#include <sys/types.h>")
        write("#include <sys/socket.h>")
        write("#include <netinet/in.h>")
        write("#include <netinet/tcp.h>")
        write("#include <arpa/inet.h>")
        write("#include <sys/ioctl.h>")
        write("#include <netdb.h>")
        write("#include <fcntl.h>")
        write("#include <errno.h>")
    }

    override def emitGlobals(streams: Traversable[Stream]) {
        streams.foreach { s =>
            if (isProducer(s)) {
                writeProducerGlobals(s)
            } else {
                writeConsumerGlobals(s)
            }
        }
    }

    override def emitInit(streams: Traversable[Stream]) {
        streams.foreach { s =>
            if (isProducer(s)) {
                writeInitProducer(s)
            } else {
                writeInitConsumer(s)
            }
        }
    }

    override def emitDestroy(streams: Traversable[Stream]) {
        streams.foreach { s =>
            if (isProducer(s)) {
                writeDestroyProducer(s)
            } else {
                writeDestroyConsumer(s)
            }
        }
    }

    private def writeProducerGlobals(stream: Stream) {

        val sock = s"sock${stream.label}"
        val bufname = s"buffer${stream.label}"
        val vtype = stream.valueType

        // Globals.
        write(s"static int $sock = 0;")
        write(s"static char *$bufname = NULL;")

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
        write(s"static size_t size = 0;")
        write(s"const size_t temp = count * sizeof($vtype);")
        writeIf(s"XUNLIKELY(temp > size)")
        write(s"size = temp;")
        write(s"$bufname = (char*)realloc($bufname, size);")
        writeEnd
        writeReturn(bufname)
        leave

        // "send"
        write(s"static void ${stream.label}_send(int count)")
        enter
        write(s"const size_t size = count * sizeof($vtype);")
        write(s"const int c = send($sock, $bufname, size, 0);")
        writeIf(s"XUNLIKELY(c < 0)")
        write("perror(\"send failed\");")
        write("exit(-1);")
        writeEnd
        leave

    }

    private def writeConsumerGlobals(stream: Stream) {

        val sock = s"sock${stream.label}"
        val lsock = s"server${stream.label}"
        val qname = s"q_${stream.label}"
        val vtype = stream.valueType
        val destIndex = stream.destIndex
        val destKernel = stream.destKernel
        val destLabel = destKernel.label
        val destName = destKernel.name

        // Globals.
        write(s"static int $sock = 0;")
        write(s"static int $lsock = 0;")
        write(s"static APQ *$qname = NULL;")

        // "process"
        write(s"static bool ${stream.label}_process()")
        enter
        write(s"static ssize_t leftovers = 0;")
        write(s"static char *ptr = NULL;")
        writeIf(s"XUNLIKELY($sock == 0)")
        write(s"struct sockaddr_in caddr;")
        write(s"socklen_t caddrlen = sizeof(caddr);")
        write(s"$sock = accept($lsock, (struct sockaddr*)&caddr, &caddrlen);")
        writeIf(s"$sock < 0")
        writeIf(s"errno == EAGAIN")
        write(s"$sock = 0;")
        writeReturn("0")
        writeEnd
        write("perror(\"accept\");")
        write("exit(-1);")
        writeEnd
        write(s"fcntl($sock, F_SETFL, O_NONBLOCK);")
        writeEnd
        write(s"size_t max_size;")
        writeIf(s"leftovers > 0")
        write(s"max_size = sizeof($vtype) - leftovers;")
        writeElse
        write(s"const size_t max_count = ${qname}->depth >> 3;")
        write(s"max_size = sizeof($vtype) * max_count;")
        write(s"ptr = (char*)APQ_StartWrite($qname, max_count);")
        writeEnd
        writeIf(s"ptr != NULL")
        write(s"ssize_t rc = recv($sock, ptr, max_size, 0);")
        writeIf(s"rc < 0")
        writeIf(s"XUNLIKELY(errno != EAGAIN)")
        write("perror(\"recv\");")
        write("exit(-1);")
        writeEnd
        writeElse
        write(s"const size_t total = rc + leftovers;")
        write(s"const size_t count = total / sizeof($vtype);")
        write(s"leftovers = total % sizeof($vtype);")
        write(s"ptr += rc;")
        write(s"APQ_FinishWrite($qname, count);")
        writeEnd
        writeEnd
        writeIf(s"$destLabel.inputs[$destIndex].data == NULL")
        write(s"char *buf;")
        write(s"uint32_t c = APQ_StartRead($qname, &buf);")
        writeIf(s"c > 0")
        write(s"$destLabel.inputs[$destIndex].data = ($vtype*)buf;")
        write(s"$destLabel.inputs[$destIndex].count = c;")
        writeEnd
        writeEnd
        writeIf(s"$destLabel.inputs[$destIndex].data")
        write(s"$destLabel.clock.count += 1;")
        write(s"ap_${destName}_push(&$destLabel.priv, $destIndex, " +
              s"$destLabel.inputs[$destIndex].data, " +
              s"$destLabel.inputs[$destIndex].count);")
        writeReturn("true")
        writeElse
        writeReturn("false")
        writeEnd
        leave

        // "release"
        write(s"static void ${stream.label}_release(int count)")
        enter
        write(s"APQ_FinishRead($qname, count);")
        leave

        // "is_empty"
        write(s"static int ${stream.label}_is_empty()")
        enter
        writeReturn(s"APQ_IsEmpty($qname)")
        leave

    }

    private def writeInitProducer(stream: Stream) {

        val sock = s"sock${stream.label}"
        val remoteHost = stream.destKernel.device.host
        val port = sp.getPort(stream)

        // Create the client socket and connect to the server.
        enter
        write(s"struct hostent *host;")
        write("host = gethostbyname(\"" + remoteHost + "\");")
        writeIf("host == NULL")
        write("perror(\"gethostbyname\");")
        write("exit(-1);")
        writeEnd
        write(s"for(;;)")
        enter
        write(s"$sock = socket(PF_INET, SOCK_STREAM, 0);")
        writeIf(s"$sock < 0")
        write("perror(\"socket\");")
        write("exit(-1);")
        writeEnd
        write(s"struct sockaddr_in addr;")
        write(s"memset(&addr, 0, sizeof(addr));")
        write(s"addr.sin_family = PF_INET;")
        write(s"addr.sin_port = $port;")
        write(s"addr.sin_addr = *(struct in_addr*)host->h_addr;")
        write(s"int rc = connect($sock, (struct sockaddr*)&addr, " +
              s"sizeof(addr));")
        writeIf("rc")
        writeIf("rc != ECONNREFUSED")
        write("perror(\"connect\");")
        write("exit(-1);")
        writeElse
        write(s"close($sock);")
        write(s"usleep(100);")
        writeEnd
        writeElse
        write("break;")
        writeEnd
        leave
        leave

    }

    private def writeInitConsumer(stream: Stream) {

        val lsock = s"server${stream.label}"
        val qname = s"q_${stream.label}"
        val depth = stream.depth
        val vtype = stream.valueType
        val port = sp.getPort(stream)

        // Initialize the queue.
        write(s"$qname = (APQ*)malloc(APQ_GetSize($depth, sizeof($vtype)));")
        write(s"APQ_Initialize($qname, $depth, sizeof($vtype));")

        // Create the server socket.
        enter
        write(s"$lsock = socket(PF_INET, SOCK_STREAM, 0);")
        writeIf(s"$lsock < 0")
        write("perror(\"socket\");")
        write("exit(-1);")
        writeEnd
        write(s"struct sockaddr_in addr;")
        write(s"memset(&addr, 0, sizeof(addr));")
        write(s"addr.sin_family = PF_INET;")
        write(s"addr.sin_port = $port;")
        write(s"int rc = bind($lsock, (struct sockaddr*)&addr, sizeof(addr));")
        writeIf(s"rc")
        write("perror(\"bind\");")
        write("exit(-1);")
        writeEnd
        write(s"rc = listen($lsock, 1);")
        writeIf(s"rc")
        write("perror(\"listen\");")
        write("exit(-1);")
        writeEnd
        leave

    }

    private def writeDestroyProducer(stream: Stream) {
        val sock = s"sock${stream.label}"
        writeIf(s"$sock")
        write(s"close($sock);")
        writeEnd
    }

    private def writeDestroyConsumer(stream: Stream) {

        val qname = s"q_${stream.label}"
        val sock = s"sock${stream.label}"
        val lsock = s"server${stream.label}"

        writeIf(s"$sock")
        write(s"close($sock);")
        writeEnd
        writeIf(s"$lsock")
        write(s"close($lsock);")
        writeEnd
        write(s"free($qname);")

    }

}
