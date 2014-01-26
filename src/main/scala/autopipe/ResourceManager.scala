package autopipe

import autopipe.gen.ResourceGenerator
import autopipe.gen.CPUResourceGenerator
import autopipe.gen.SmartFusionResourceGenerator
import autopipe.gen.SimulationResourceGenerator
import autopipe.gen.OpenCLResourceGenerator

private[autopipe] class ResourceManager(val ap: AutoPipe) {

    private var generators = Map[Device, ResourceGenerator]()
    private var cpuGenerators = Map[String, CPUResourceGenerator]()

    private def create(device: Device): ResourceGenerator = {
        val platform = device.deviceType.platform
        platform match {
            case Platforms.HDL => createHDLResourceGenerator(device)
            case Platforms.OpenCL =>
                new OpenCLResourceGenerator(ap, device)
            case _ =>
                sys.error("unknown platform: " + platform)
        }
    }

    private def createHDLResourceGenerator(device: Device): ResourceGenerator = {
        val fpga = ap.parameters.get[String]('fpga)
        fpga match {
            case "SmartFusion"    =>
                new SmartFusionResourceGenerator(ap, device)
            case "Simulation"     =>
                new SimulationResourceGenerator(ap, device)
            case _ => sys.error("unknown FPGA device: " + fpga)
        }
    }

    def get(device: Device): ResourceGenerator = {
        if (device.deviceType.platform == Platforms.C) {
            val host = device.host
            if (cpuGenerators.contains(host)) {
                cpuGenerators(host)
            } else {
                val cg = new CPUResourceGenerator(ap, host)
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

