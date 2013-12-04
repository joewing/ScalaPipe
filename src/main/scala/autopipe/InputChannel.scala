
package autopipe

import autopipe.dsl.{AutoPipeApp, AutoPipeBlock, AutoPipeType}
import blocks.InputSocket
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.Channels

/** Channel for writing to a generated application. */
class InputChannel(_t: AutoPipeType) extends Channel(_t) {

   private lazy val stream    = client.getOutputStream()
   private lazy val channel   = Channels.newChannel(stream)

   def apply()(implicit ap: AutoPipe): StreamList = {
      val apb = new InputSocket(t, port)
      val instance = ap.createBlock(apb)
      instance.apply()
   }

   def write(value: t.T) {
      t.write(buffer, value)
      buffer.flip()
      channel.write(buffer)
      buffer.clear()
   }

}

