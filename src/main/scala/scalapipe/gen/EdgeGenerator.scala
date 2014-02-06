package scalapipe.gen

import scalapipe._
import scalapipe.dsl.Stream

private[scalapipe] abstract class EdgeGenerator(
        val platform: Platforms.Value
    ) extends Generator {

    protected def getDevices(streams: Traversable[Stream]):
        Traversable[Device] = {

        streams.map { s =>
            if (s.sourceKernel.device.platform == platform) {
                s.sourceKernel.device
            } else {
                s.destKernel.device
            }
        }

    }

    protected def getSenderStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.destKernel.device == device && s.sourceKernel.device != device
        }
    }

    protected def getReceiverStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device != device
        }
    }

    protected def getInternalStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.sourceKernel.device == device && s.destKernel.device == device
        }
    }

    protected def getExternalStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        getSenderStreams(device, streams) ++ getReceiverStreams(device, streams)
    }

    protected def getKernels(device: Device,
                             kernels: Traversable[KernelInstance]) = {
        kernels.filter(_.device == device)
    }

    /** Emit common code. */
    def emitCommon() {
    }

    /** Emit device globals. */
    def emitGlobals(streams: Traversable[Stream]) {
    }

    /** Emit device initialization code. */
    def emitInit(streams: Traversable[Stream]) {
    }

    /** Emit device shutdown code. */
    def emitDestroy(streams: Traversable[Stream]) {
    }

    /** Emit statistics code. */
    def emitStats(streams: Traversable[Stream]) {
    }

}
