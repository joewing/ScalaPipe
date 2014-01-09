
package autopipe

import autopipe.dsl.{AutoPipeBlock, AutoPipeType}
import blocks.OutputSocket
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.Channels

/** Channel for reading from a generated application. */
class OutputChannel(_t: AutoPipeType) extends Channel(_t) {

    type T = t.T

    private lazy val stream     = client.getInputStream()
    private lazy val channel    = Channels.newChannel(stream)

    def apply(s: Stream)(implicit ap: AutoPipe): StreamList = {
        val apb = new OutputSocket(t, port)
        val instance = ap.createBlock(apb)
        instance.apply((null, s))
    }

    def read[A](): A = {
        while (buffer.position() < elementSize) {
            channel.read(buffer)
        }
        buffer.flip()
        val result = t.read(buffer)
        buffer.compact()
        result.asInstanceOf[A]
    }


}

