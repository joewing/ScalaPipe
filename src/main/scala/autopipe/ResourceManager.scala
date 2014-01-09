
package autopipe

import autopipe._
import scala.collection.mutable.HashMap

import autopipe.gen.ResourceGenerator
import autopipe.gen.CPUResourceGenerator
import autopipe.gen.SmartFusionResourceGenerator
import autopipe.gen.SimulationResourceGenerator
import autopipe.gen.OpenCLResourceGenerator

private[autopipe] class ResourceManager(val ap: AutoPipe) {

    private val generators = new HashMap[Device, ResourceGenerator]
    private val cpuGenerators = new HashMap[String, CPUResourceGenerator]

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
            case "SmartFusion"    => new SmartFusionResourceGenerator(ap, device)
            case "Simulation"     => new SimulationResourceGenerator(ap, device)
            case _ => sys.error("unknown FPGA device: " + fpga)
        }
    }

    def get(device: Device): ResourceGenerator = {
        if (device.deviceType.platform == Platforms.C) {
            val host = device.host
            cpuGenerators.getOrElseUpdate(host, {
                new CPUResourceGenerator(ap, host)
            })
        } else {
            generators.getOrElseUpdate(device, { create(device) } )
        }
    }

}

