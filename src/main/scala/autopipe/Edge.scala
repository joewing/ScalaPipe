
package autopipe

private[autopipe] abstract class Edge(
        val source: Platforms.Value,
        val dest: DeviceSpec
    ) {

    private[autopipe] val label = LabelMaker.getDeviceLabel

    private[autopipe] var queueSize = 0

    private[autopipe] def defaultSource: DeviceSpec =
        new DeviceSpec(source, null, Int.MaxValue)

}

private[autopipe] class EdgeObject[T <: Edge](
        val platform: Platforms.Value
    )(implicit m: Manifest[T]) {

    def apply(host: String = null,
                 id: Int = Int.MaxValue,
                 queueSize: Int = 0): T = {
        val c = m.runtimeClass.getConstructor(classOf[DeviceSpec])
        val o = c.newInstance(new DeviceSpec(platform, host, id))
        val t = o.asInstanceOf[T]
        t.queueSize = queueSize
        t
    }

}

object CPU2FPGA extends EdgeObject[CPU2FPGA](Platforms.HDL)

private[autopipe] class CPU2FPGA(_dest: DeviceSpec)
    extends Edge(Platforms.C, _dest)

object FPGA2CPU extends EdgeObject[FPGA2CPU](Platforms.C)

private[autopipe] class FPGA2CPU(_dest: DeviceSpec)
    extends Edge(Platforms.HDL, _dest)

object CPU2CPU extends EdgeObject[CPU2CPU](Platforms.C)

private[autopipe] class CPU2CPU(_dest: DeviceSpec)
    extends Edge(Platforms.C, _dest)

object CPU2GPU extends EdgeObject[CPU2GPU](Platforms.OpenCL)

private[autopipe] class CPU2GPU(_dest: DeviceSpec)
    extends Edge(Platforms.C, _dest)

object GPU2CPU extends EdgeObject[GPU2CPU](Platforms.C)

private[autopipe] class GPU2CPU(_dest: DeviceSpec)
    extends Edge(Platforms.OpenCL, _dest)

