
package autopipe

import scala.collection.mutable.ListBuffer

private[autopipe] class Device(
      val deviceType: DeviceType,
      val host: String,
      var index: Int) {

   private[autopipe] val label = LabelMaker.getDeviceLabel
   private[autopipe] val name: String = deviceType + "[" + index + "]"
   private[autopipe] val procName: String = "proc_" + (index + 1) + "_"
   private[autopipe] val platform = deviceType.platform
   private val blocks = new ListBuffer[Block]

   private[autopipe] def addBlock(b: Block) {
      blocks += b
   }

   def emitResource: String = platform match {
      case Platforms.HDL =>
         "HDL(file=\"fpga_x.vhd\", " +
         "wrapfile=\"fpga_wrap.vhd\", " +
         "topfile=\"top0.v\")"
      case Platforms.C =>
         "C_x86(file=\"proc_" + (index + 1) + "_.cpp\", " +
         "cpunum=" + index + ")"
      case Platforms.OpenCL =>
         "OpenCL()"
      case _ => sys.error("invalid platform")
   }

   private[autopipe] def emit: String = {
      "resource " + label + " is " + emitResource + ";\n" +
      "map " + label + " = { " + blocks.foldLeft("") { (a, b) =>
         if(!a.isEmpty()) {
            a + ", app." + b.label
         } else {
            "app." + b.label
         }
      } + " };\n"
   }

   override def toString =
      deviceType.toString + "(" + host + ", " + index + ")"

}

