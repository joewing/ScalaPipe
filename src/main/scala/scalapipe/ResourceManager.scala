package scalapipe

import scalapipe.gen.ResourceGenerator
import scalapipe.gen.CPUResourceGenerator
import scalapipe.gen.SmartFusionResourceGenerator
import scalapipe.gen.SimulationResourceGenerator
import scalapipe.gen.OpenCLResourceGenerator

private[scalapipe] class ResourceManager(val sp: ScalaPipe) {

    private var generators = Map[Device, ResourceGenerator]()
    private var cpuGenerators = Map[String, CPUResourceGenerator]()

    private def create(device: Device): ResourceGenerator = {
        val platform = device.deviceType.platform
        platform match {
            case Platforms.HDL => createHDLResourceGenerator(device)
            case Platforms.OpenCL =>
                new OpenCLResourceGenerator(sp, device)
            case _ =>
                sys.error("unknown platform: " + platform)
        }
    }

    private def createHDLResourceGenerator(device: Device): ResourceGenerator = {
        val fpga = sp.parameters.get[String]('fpga)
        fpga match {
            case "SmartFusion"    =>
                new SmartFusionResourceGenerator(sp, device)
            case "Simulation"     =>
                new SimulationResourceGenerator(sp, device)
            case _ => sys.error("unknown FPGA device: " + fpga)
        }
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

