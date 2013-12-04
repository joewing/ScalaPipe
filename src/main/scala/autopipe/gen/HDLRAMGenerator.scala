
package autopipe.gen

import autopipe._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap

private[gen] class HDLRAMGenerator(
      val ap: AutoPipe,
      val name: String
   ) extends HDLGenerator {

   private var nextIndex = 0
   private var nextOffset = 0

   private def getIndex: Int = {
      val result = nextIndex
      nextIndex += 1
      result
   }

   private def getOffset(size: Int): Int = {
      val result = nextOffset
      nextOffset += size
      result
   }

   private class RAMPort(val index: Int)

   private class RAM(val index: Int,
                     val width: Int,
                     val size: Int,
                     val offset: Int) {
      val readPorts  = new ListBuffer[RAMPort]
      val writePorts = new ListBuffer[RAMPort]
   }

   private val rams = new HashMap[Int, RAM]

   def addMemory(width: Int, size: Int): Int = {
      val index = getIndex
      val offset = getOffset(size)
      rams += ((index, new RAM(index, width, size, offset)))
      index
   }

   def addReadPort(index: Int): String = {
      val ram = rams(index)
      val pindex = getIndex
      ram.readPorts += new RAMPort(pindex)
      "port" + pindex
   }

   def addWritePort(index: Int): String = {
      val ram = rams(index)
      val pindex = getIndex
      ram.writePorts += new RAMPort(pindex)
      "port" + pindex
   }

   def maxWidth: Int = rams.values.foldLeft(0) {
      (a, p) => math.max(a, p.width)
   }

   def totalSize: Int = rams.values.foldLeft(0) { (a, p) => a + p.size }

   private def emitShared {

      set("name", "ram0")
      set("width", maxWidth)
      set("size", totalSize)
      write("reg [$width - 1$:0] $name$ [0:$size - 1$];")

      // Declare ports.
      rams.values.foreach { r =>
         r.readPorts.foreach { p =>
            set("port", "port" + p.index)
            write("wire [31:0] $port$_index;")
            write("reg [$width - 1$:0] $port$_out;")
            write("wire $port$_re;")
            write("reg $port$_done;")
         }
         r.writePorts.foreach { p =>
            set("port", "port" + p.index)
            write("wire [31:0] $port$_index;")
            write("wire [$width - 1$:0] $port$_in;")
            write("wire $port$_we;")
            write("reg $port$_done;")
         }
      }
   
      // Implement read ports.
      write("reg [$width - 1$:0] $name$_out;")
      write("reg [31:0] $name$_read_index;")
      write("always @(*) begin")
      enter
      rams.values.foreach { r =>

         set("offset", r.offset)
         r.readPorts.foreach { p =>
            set("port", "port" + p.index)
            write("$port$_done <= 0;")
            write("$port$_out <= $name$_out;")
         }

         set("port", "port" + r.readPorts.head.index)
         write("if ($port$_re) begin")
         enter
         write("$port$_done <= 1;")
         write("$name$_read_index <= $port$_index + $offset$;")
         leave
         r.readPorts.tail.foreach { p =>
            set("port", "port" + p.index)
            write("end else if ($port$_re) begin")
            enter
            write("$port$_done <= 1;")
            write("$name$_read_index <= $port$_index + $offset$;")
            leave
         }
         write("end")

      }
      leave
      write("end")
      write("always @(posedge clk) begin")
      enter
      write("$name$_out <= $name$[$name$_read_index];")
      leave
      write("end")
      write

      // Implement write ports.
      write("reg [$width - 1$:0] $name$_in;")
      write("reg [31:0] $name$_write_index;")
      write("reg $name$_we;")
      write("always @(*) begin")
      enter
      rams.values.foreach { r =>

         set("offset", r.offset)
         write("$name$_we <= 0;")
         r.writePorts.foreach { p =>
            set("port", "port" + p.index)
            write("$port$_done <= 0;")
         }

         set("port", "port" + r.writePorts.head.index)
         write("if ($port$_we) begin")
         enter
         write("$port$_done <= 1;")
         write("$name$_write_index <= $port$_index + $offset$;")
         write("$name$_in <= $port$_in;")
         leave
         r.writePorts.tail.foreach { p =>
            set("port", "port" + p.index)
            write("end else if ($port$_we) begin")
            enter
            write("$port$_done <= 1;")
            write("$name$_write_index <= $port$_index + $offset$;")
            write("$name$_in <= $port$_in;")
            leave
         }
         write("end")

      }
      leave
      write("end")
      write("always @(posedge clk) begin")
      enter
      write("if ($name$_we) begin")
      enter
      write("$name$[$name$_write_index] <= $name$_in;")
      leave
      write("end")
      leave
      write("end")
      write

   }

   private def emitIndependent {

      rams.values.foreach { r =>

         set("name", "ram" + r.index)
         set("width", r.width)
         set("size", r.size)
         write("reg [$width - 1$:0] $name$ [0:$size - 1$];")

         r.readPorts.foreach { p =>
            set("port", "port" + p.index)
            write("wire [31:0] $port$_index;")
            write("reg [$width - 1$:0] $port$_out;")
            write("wire $port$_re;")
            write("reg $port$_done;")
            write
            write("always @(posedge clk) begin")
            enter
            write("$port$_done <= $port$_re;")
            write("$port$_out <= $name$[$port$_index];")
            leave
            write("end")
         }

         r.writePorts.foreach { p =>
            set("port", "port" + p.index)
            write("wire [31:0] $port$_index;")
            write("wire [$width - 1$:0] $port$_in;")
            write("wire $port$_we;")
            write("reg $port$_done;")
            write
            write("always @(posedge clk) begin")
            enter
            write("$port$_done <= $port$_we;")
            write("if ($port$_we) begin")
            enter
            write("$name$[$port$_index] <= $port$_in;")
            leave
            write("end")
            leave
            write("end")
         }


      }

   }

   def emit {
      emitIndependent
   }

}

