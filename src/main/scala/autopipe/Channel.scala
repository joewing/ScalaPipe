
package autopipe

import autopipe.dsl.AutoPipeType
import java.net.{ServerSocket, Socket}
import java.nio.{ByteBuffer, ByteOrder}

abstract class Channel(val t: AutoPipeType) {

    protected val elementSize  = (t.create().bits + 7) / 8
    private val bufferSize      = 8 * elementSize
    protected val buffer         = ByteBuffer.allocateDirect(bufferSize)
    private val server = new ServerSocket(0, 1, null)
    protected val port = server.getLocalPort()
    protected lazy val client = server.accept()

    buffer.order(ByteOrder.nativeOrder())
    server.setReuseAddress(true)

}

