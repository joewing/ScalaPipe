
package autopipe.gen

import autopipe._

private[autopipe] abstract class EdgeGenerator(val platform: Platforms.Value)
     extends Generator {

    protected def getDevices(streams: Traversable[Stream]):
        Traversable[Device] = {

        streams.map { s =>
            if (s.sourceBlock.device.platform == platform) {
                s.sourceBlock.device
            } else {
                s.destBlock.device
            }
        }

    }

    protected def getSenderStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.destBlock.device == device && s.sourceBlock.device != device
        }
    }

    protected def getReceiverStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.sourceBlock.device == device && s.destBlock.device != device
        }
    }

    protected def getInternalStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        streams.filter { s =>
            s.sourceBlock.device == device && s.destBlock.device == device
        }
    }

    protected def getExternalStreams(device: Device,
        streams: Traversable[Stream]): Traversable[Stream] = {
        getSenderStreams(device, streams) ++ getReceiverStreams(device, streams)
    }

    protected def getBlocks(device: Device, blocks: Traversable[Block]):
        Traversable[Block] = {
        blocks.filter { _.device == device }
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

