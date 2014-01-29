package scalapipe

private[scalapipe] abstract class Edge(
        val source: Platforms.Value,
        val dest: DeviceSpec
    ) {

    private[scalapipe] val label = LabelMaker.getDeviceLabel

    private[scalapipe] var queueSize = 0

    private[scalapipe] def defaultSource: DeviceSpec =
        new DeviceSpec(source, null, Int.MaxValue)

}

private[scalapipe] class EdgeObject[T <: Edge](
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

class CPU2FPGA(_dest: DeviceSpec) extends Edge(Platforms.C, _dest)

class FPGA2CPU(_dest: DeviceSpec) extends Edge(Platforms.HDL, _dest)

class CPU2CPU(_dest: DeviceSpec) extends Edge(Platforms.C, _dest)

class CPU2GPU(_dest: DeviceSpec) extends Edge(Platforms.C, _dest)

class GPU2CPU(_dest: DeviceSpec) extends Edge(Platforms.OpenCL, _dest)
