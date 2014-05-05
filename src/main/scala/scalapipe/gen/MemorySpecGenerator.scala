package scalapipe.gen

import scalapipe._
import java.io.File

private[scalapipe] class MemorySpecGenerator(
        val sp: ScalaPipe,
        val device: Device
    ) extends Generator {

    protected val streams = sp.streams.filter { s =>
        (s.destKernel.kernelType.pure && s.destKernel.device == device) ||
        (s.sourceKernel.kernelType.pure && s.sourceKernel.device == device)
    }.toSeq.sortBy { s => s.index }

    protected val kernels = sp.instances.filter { k =>
        k.device == device && k.kernelType.pure
    }.toSeq.sortBy { k => k.index }

    def emit(dir: File) {

        if (kernels.isEmpty) {
            return
        }

        write(s"(memory")
        enter
        write("(main (memory (dram)))")
        for (k <- kernels) {
            val id = k.index
            val wordSize = sp.parameters.get[Int]('memoryWidth) / 8
            val depth = k.kernelType.ramDepth
            write(s"(subsystem (id $id)(depth $depth)(word_size $wordSize)")
            enter
            write(s"(memory (main))")
            leave
            write(s")")
        }
        for (s <- streams) {
            val id = s.index
            val depth = s.parameters.get[Int]('fpgaQueueDepth)
            val itemSize = s.valueType.bytes
            val sid = s.sourceKernel.index
            val did = s.destKernel.index
            write(s"(fifo (id $id)(depth $depth)(word_size $itemSize)")
            enter
            write(s"; $sid -> $did")
            write(s"(memory (main))")
            leave
            write(s")")
        }
        leave
        write(s")")
        write(s"(benchmarks")
        enter
        for (k <- kernels) {
            val id = k.index
            val name = s"${k.name}${k.index}"
            write(s"(trace (id $id)(name $name))")
        }
        leave
        write(s")")

        val filename = s"mem_${device.label}.mem"
        writeFile(dir, filename)

    }

}
