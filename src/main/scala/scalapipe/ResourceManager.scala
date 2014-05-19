package scalapipe

import scala.collection.mutable.{HashMap, HashSet}
import scalapipe.gen.ResourceGenerator
import scalapipe.gen.CPUResourceGenerator
import scalapipe.gen.SmartFusionResourceGenerator
import scalapipe.gen.SimulationResourceGenerator
import scalapipe.gen.SaturnResourceGenerator
import scalapipe.gen.OpenCLResourceGenerator

private[scalapipe] class ResourceManager(val sp: ScalaPipe) {

    private lazy val basePort = sp.parameters.get[Int]('basePort)
    private var portMap = Map[Stream, Int]()
    private val usedPorts = new HashMap[String, HashSet[Int]]()
    private var generators = Map[Device, ResourceGenerator]()
    private var cpuGenerators = Map[String, CPUResourceGenerator]()

    private def create(device: Device): ResourceGenerator = {
        val platform = device.deviceType.platform
        platform match {
            case Platforms.HDL => hdlResourceGenerator(device)
            case Platforms.OpenCL => new OpenCLResourceGenerator(sp, device)
            case _ => sys.error("unknown platform: " + platform)
        }
    }

    private def hdlResourceGenerator(device: Device): ResourceGenerator = {
        val fpga = sp.parameters.get[String]('fpga)
        fpga match {
            case "SmartFusion"    =>
                new SmartFusionResourceGenerator(sp, device)
            case "Simulation"     =>
                new SimulationResourceGenerator(sp, device)
            case "Saturn" =>
                new SaturnResourceGenerator(sp, device)
            case _ => sys.error("unknown FPGA device: " + fpga)
        }
    }

    private def usesPort(host: String, port: Int): Boolean = {
        val ports = usedPorts.getOrElseUpdate(host, new HashSet[Int])
        ports.contains(port)
    }

    def getPort(stream: Stream): Int = portMap.get(stream) match {
        case Some(p) =>
            return p
        case None =>
            val hosta = stream.sourceKernel.device.host
            val hostb = stream.destKernel.device.host
            var port = basePort
            while (usesPort(hosta, port) || usesPort(hostb, port)) {
                port += 1
            }
            usedPorts(hosta) += port
            usedPorts(hostb) += port
            portMap += (stream -> port)
            return port
    }

    def get(device: Device): ResourceGenerator = {
        if (device.deviceType.platform == Platforms.C) {
            val host = device.host
            if (cpuGenerators.contains(host)) {
                cpuGenerators(host)
            } else {
                val cg = new CPUResourceGenerator(sp, host)
                cpuGenerators += (host -> cg)
                cg
            }
        } else if (generators.contains(device)) {
            generators(device)
        } else {
            val g = create(device)
            generators += (device -> g)
            g
        }
    }

}
