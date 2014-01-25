package autopipe.gen

import autopipe._

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import java.io.File

private[autopipe] class CPUResourceGenerator(
        val ap: AutoPipe,
        val host: String
    ) extends ResourceGenerator {

    private val edgeGenerators = new HashMap[EdgeGenerator, HashSet[Stream]]
    private val emittedKernelTypes = new HashSet[KernelType]
    private val threadIds = new HashMap[Kernel, Int]

    private lazy val openCLEdgeGenerator = new OpenCLEdgeGenerator(ap)
    private lazy val smartFusionEdgeGenerator = new SmartFusionEdgeGenerator(ap)
    private lazy val simulationEdgeGenerator = new SimulationEdgeGenerator(ap)
    private lazy val sockEdgeGenerator = new SockEdgeGenerator
    private lazy val cEdgeGenerator = new CEdgeGenerator

    private def getHDLEdgeGenerator: EdgeGenerator = {
        val fpga = ap.parameters.get[String]('fpga)
        fpga match {
            case "SmartFusion"    => smartFusionEdgeGenerator
            case "Simulation"     => simulationEdgeGenerator
            case _ =>
                Error.raise("unknown FPGA type: " + fpga)
                simulationEdgeGenerator
        }
    }

    private def addEdgeGenerator(stream: Stream) {

        val dest = stream.destKernel.device
        val src = stream.sourceKernel.device

        val generator: EdgeGenerator = stream.edge match {
            case c2f: CPU2FPGA                          => getHDLEdgeGenerator
            case f2c: FPGA2CPU                          => getHDLEdgeGenerator
            case c2g: CPU2GPU                           => openCLEdgeGenerator
            case g2c: GPU2CPU                           => openCLEdgeGenerator
            case c2c: CPU2CPU if dest.host != src.host  => sockEdgeGenerator
            case _                                      => cEdgeGenerator
        }

        if (!edgeGenerators.contains(generator)) {
            edgeGenerators += ((generator, new HashSet[Stream]))
        }
        edgeGenerators(generator) += stream

    }

    private def shouldEmit(device: Device): Boolean = {
        device.platform == Platforms.C && device.host == host
    }

    private def emitKernelHeader(kernel: Kernel) {
        val kernelType = kernel.kernelType
        if (!emittedKernelTypes.contains(kernelType)) {
            emittedKernelTypes += kernelType
            write("#include \"" + kernelType.name + "/" +
                  kernelType.name + ".h\"")
        }
    }

    private def emitKernelStruct(kernel: Kernel) {

        val inputCount = kernel.getInputs.size
        val outputCount = kernel.getOutputs.size

        write("static struct {")
        enter
        write("APC clock;")
        write("volatile int active_inputs;")
        write("AP_input_port inputs[" + inputCount + "];")
        write("AP_output_port outputs[" + outputCount + "];")
        write("AP_block_data data;")
        write("struct ap_" + kernel.kernelType.name + "_data priv;")
        leave
        write("} " + kernel.label + ";")

    }

    private def emitKernelInit(kernel: Kernel) {

        val name = kernel.name
        val kernelType = kernel.kernelType
        val instance = kernel.label

        var minDepth = 0

        // Input ports
        for (stream <- kernel.getInputs) {
            val index = kernel.inputIndex(stream.destPort)
            write(instance + ".inputs[" + index + "].data = NULL;")
            write(instance + ".inputs[" + index + "].count = 0;")
            write(instance + ".inputs[" + index + "].credit = 0;")
            write(instance + ".inputs[" + index + "].stop = false;")
            if (minDepth == 0 || stream.depth < minDepth) {
                minDepth = stream.depth
            }
        }
        val inPortCount = kernel.getInputs.size
        val activeInputs = inPortCount + 1
        write(instance + ".active_inputs = " + activeInputs + ";")

        // Output ports
        for (stream <- kernel.getOutputs) {
            val index = kernel.outputIndex(stream.sourcePort)
            write(instance + ".outputs[" + index + "].data = NULL;")
            write(instance + ".outputs[" + index + "].count = 0;")
            write(instance + ".outputs[" + index + "].credit = 0;")
            if (minDepth == 0 || stream.depth < minDepth) {
                minDepth = stream.depth
            }
        }
        val outPortCount = kernel.getOutputs.size

        // AP_block_data
        val maxSendCount = minDepth / 2
        write(instance + ".data.in_port_count = " + inPortCount + ";")
        write(instance + ".data.in_ports = " + instance + ".inputs;")
        write(instance + ".data.out_port_count = " + outPortCount + ";")
        write(instance + ".data.out_ports = " + instance + ".outputs;")
        write(instance + ".data.max_send_count = " + maxSendCount + ";")
        write(instance + ".data.get_free = " + instance + "_get_free;")
        write(instance + ".data.allocate = " + instance + "_allocate;")
        write(instance + ".data.send = " + instance + "_send;")
        write(instance + ".data.release = " + instance + "_release;")
        write(instance + ".data.send_signal = " + instance + "_send_signal;")
        write(instance + ".data.instance = " + kernel.index + ";")

        // Default config options.
        for (c <- kernel.kernelType.configs) {
            val name = c.name
            val t = c.valueType.baseType
            val custom = kernel.getConfig(name)
            val value = if (custom != null) custom else c.value
            if (value != null) {
                write(instance + ".priv." + name + " = (" + t + ")" +
                      kernel.kernelType.getLiteral(value) + ";")
            }
        }

        // Call the init function.
        write("APC_Init(&" + instance + ".clock);")
        write("ap_" + name + "_init(&" + instance + ".priv);")

    }

    private def emitKernelGetFree(kernel: Kernel) {

        val instance = kernel.label

        write("static int " + instance + "_get_free(int out_port)")
        write("{")
        enter
        write("switch(out_port) {")
        for (stream <- kernel.getOutputs) {
            val index = kernel.outputIndex(stream.sourcePort)
            write("case " + index + ":")
            enter
            write("return " + stream.label + "_get_free();")
            leave
        }
        write("}")
        write("return 0;")
        leave
        write("}")

    }

    private def emitKernelAllocate(kernel: Kernel) {

        val instance = kernel.label
        val rid = threadIds(kernel)

        write("static void *" + instance + "_allocate(int out_port, int count)")
        write("{")
        enter
        write("void *ptr = NULL;")
        if (!kernel.getOutputs.filter { _.useFull }.isEmpty) {
            write("bool first = true;")
        }
        write("while(ptr == NULL) {")
        enter

        write("switch(out_port) {")
        for (stream <- kernel.getOutputs) {
            val index = kernel.outputIndex(stream.sourcePort)
            write("case " + index + ":")
            enter
            write("ptr = " + stream.label + "_allocate(count);")
            write(instance + ".outputs[" + index + "].count = count;")
            write(instance + ".outputs[" + index + "].data = ptr;")
            if (stream.useFull) {
                write("if(first && ptr == NULL) {")
                enter
                write("first = false;")
                write("tta.LogEvent(" + rid + ", " + stream.index + ", " +
                        "XTTA_TYPE_FULL);")
                leave
                write("}")
            }
            write("break;")
            leave
        }
        write("}")
        write("if(ptr == NULL) {")
        enter
        write("APC_Stop(&" + instance + ".clock);")
        write("sched_yield();");
        write("APC_Start(&" + instance + ".clock);")
        leave
        write("}")
        leave
        write("}")
        write("return ptr;")
        leave
        write("}")

    }

    private def emitKernelSend(kernel: Kernel) {

        val instance = kernel.label
        val minDepth = kernel.getOutputs.foldLeft(Int.MaxValue) {
            (a, s) => scala.math.min(a, s.depth)
        }
        val rid = threadIds(kernel)

        write("static void " + instance + "_send(int out_port, int count)")
        write("{")
        enter
        val outputCount = kernel.getOutputs.size
        if (outputCount > 0) {
            write("switch(out_port) {")
            for (stream <- kernel.getOutputs) {
                val index = kernel.outputIndex(stream.sourcePort)
                write("case " + index + ":")
                enter
                if (stream.usePush) {
                    write("for(int i = 0; i < count; i++) {")
                    enter
                    write("tta.LogEvent(" + rid + ", " + stream.index + ", " +
                          "XTTA_TYPE_PUSH, " + stream.label +
                          "_get_free() == 0);")
                    leave
                    write("}")
                }
                write(stream.label + "_send(count);")
                write("break;")
                leave
            }
            write("}")
            write("APC_Start(&" + instance + ".clock);")
        }
        leave
        write("}")

    }

    private def emitKernelRelease(kernel: Kernel) {

        val name = kernel.kernelType.name
        val instance = kernel.label

        write("static void " + instance + "_release(int in_port, int count)")
        write("{")
        enter
        write("switch(in_port) {")
        for (stream <- kernel.getInputs) {
            val index = kernel.inputIndex(stream.destPort)
            write("case " + index + ":")
            enter
            if (stream.usePop) {
                val rid = threadIds(kernel)
                write("for(int i = 0; i < count; i++) {")
                enter
                write("tta.LogEvent(" + rid + ", " + stream.index + ", " +
                        "XTTA_TYPE_POP, " + stream.label + "_get_free() == 0);")
                leave
                write("}")
            }
            write(stream.label + "_release(count);")
            write(instance + ".inputs[" + index + "].data = NULL;")
            write(instance + ".inputs[" + index + "].count = 0;")
            write("break;")
            leave
        }
        write("}")
        leave
        write("}")

    }

    private def emitKernelSendSignal(kernel: Kernel) {

        val instance = kernel.label

        write("static void " + instance + "_send_signal(int out_port, " +
                "int type, int value)")
        write("{")
        enter
        write("UNSIGNED64 temp = (UNSIGNED64)type << 56;")
        write("temp |= *(UNSIGNED32*)&value;")
        write("switch(out_port) {")
        for (stream <- kernel.getOutputs) {
            val index = kernel.outputIndex(stream.sourcePort)
            write("case " + index + ":")
            enter
            write(stream.label + "_send_signal(temp);")
            write("break;")
            leave
        }
        write("}")
        leave
        write("}")

    }

    private def emitKernelDestroy(kernel: Kernel) {
        val name = kernel.kernelType.name
        val instance = kernel.label
        write("ap_" + name + "_destroy(&" + instance + ".priv);")
    }

    private def emitCheckRunning(kernel: Kernel) {
        val id = threadIds(kernel)
        val instance = kernel.label
        write("static char is_running" + id + "()")
        write("{")
        enter
        write("if(XLIKELY(" + instance + ".active_inputs > 0)) {")
        enter
        write("return 1;")
        leave
        write("}")
        for (stream <- kernel.getInputs) {
            if (stream.sourceKernel.device == kernel.device) {
                write("if(!" + stream.label + "_is_empty()) {")
                enter
                write("return 1;")
                leave
                write("}")
            }
        }
        write("return 0;")
        leave
        write("}")
    }

    private def emitThread(kernel: Kernel) {

        val id = threadIds(kernel)
        val name = kernel.kernelType.name
        val instance = kernel.label

        write("static void *run_thread" + id + "(void *arg)")
        write("{")
        enter
        write("unsigned int count = 0;")
        write("int retval = 0;")
        write("APC_Start(&" + instance + ".clock);")
        write("while(XLIKELY(is_running" + id + "())) {")
        enter
        write("if(retval == 0) {")
        enter
        write("retval = ap_" + name + "_go(&" + instance + ".priv);")
        write("count = 0;")
        write("if(XUNLIKELY(retval)) {")
        enter
        write(instance + ".active_inputs -= 1;")
        leave
        write("}")
        leave
        write("}")
        write("count += 1;")
        if (!kernel.getInputs.isEmpty) {
            for (stream <- kernel.getInputs) {
                write("if(" + stream.label + "_process()) {")
                enter
                write("count = 0;")
                leave
                write("}")
            }
            write("if(XUNLIKELY(count > MAX_POLL_COUNT)) {")
            enter
            write("APC_Stop(&" + instance + ".clock);")
            write("sched_yield();")
            write("APC_Start(&" + instance + ".clock);")
            leave
            write("}")
        }
        leave
        write("}")
        write("APC_Stop(&" + instance + ".clock);")
        for (stream <- kernel.getOutputs) {
            if (stream.destKernel.device == kernel.device) {
                val dest = stream.destKernel.label
                write(dest + ".active_inputs -= 1;")
            }
        }
        write("return NULL;")
        leave
        write("}")

    }

    private def writeShutdown(kernels: Traversable[Kernel],
                              edgeStats: ListBuffer[Generator]) {

        def writeKernelStats(k: Kernel) {
            write("ticks = " + k.label + ".clock.total_ticks;");
            write("pushes = " + k.label + ".clock.count;")
            write("us = (ticks * total_us) / total_ticks;")
            write("run = " + k.label + ".active_inputs > 0;")
            write("fprintf(stderr, \"     " + k.kernelType.name + "(" +
                  k.label + "): %llu ticks, %llu pushes, %llu us (%s)\\n\", " +
                  "ticks, pushes, us, run ? \"run\" : \"stop\");")
            if (k.kernelType.parameters.get('profile)) {
                write("fprintf(stderr, \"        HDL Clocks: %lu\\n\", " +
                      k.label + ".priv.ap_clocks);")
            }
            write("if (show_extra_stats) {")
            enter
            k.getInputs.foreach { i =>
                val index = k.inputIndex(i)
                write("q_size = q_" + i.label + "->depth;")
                write("q_usage = APQ_GetUsed(q_" + i.label + ");")
                write("fprintf(stderr, \"          Input " + index + ": " +
                      "%llu / %llu\\n\", q_usage, q_size);")
            }
            leave
            write("}")
        }

        write("static void showStats()")
        write("{")
        enter
        write("struct timeval stop_time;")
        write("unsigned long long q_usage;")
        write("unsigned long long q_size;")
        write("unsigned long long ticks;")
        write("unsigned long long pushes;")
        write("unsigned long long us;")
        write("unsigned long long total_ticks;")
        write("unsigned long long total_us;")
        write("unsigned long long stop_ticks = xrdtsc();")
        write("bool run;")
        write("gettimeofday(&stop_time, NULL);")
        write("total_ticks = stop_ticks - start_ticks;")
        write("total_us = (stop_time.tv_sec - start_time.tv_sec) * 1000000")
        write("            + (stop_time.tv_usec - start_time.tv_usec);")
        write("""fprintf(stderr, "Statistics:\n");""")
        write("fprintf(stderr, \"Total CPU ticks: %llu\\n\", total_ticks);")
        write("fprintf(stderr, \"Total time:        %llu us\\n\", total_us);")
        kernels.foreach(k => writeKernelStats(k))
        write(edgeStats)
        leave
        write("}")

        write("static void shutdown(int s)")
        write("{")
        enter
        write("show_extra_stats = true;")
        write("""fprintf(stderr, "Shutting down...\n");""")
        write("exit(0);")
        leave
        write("}")

    }

    override def getRules: String = ""

    override def emit(dir: File) {

        // Get devices on this host.
        val localKernels = ap.kernels.filter { k =>
            k.device != null && k.device.host == host
        }
        val cpuKernels = localKernels.filter {
            k => shouldEmit(k.device)
        }
        threadIds ++= cpuKernels.zipWithIndex

        // Write include files that we need.
        write("#include \"X.h\"")
        write("#include <pthread.h>")
        write("#include <stdio.h>")
        write("#include <stdlib.h>")
        write("#include <sched.h>")
        write("#include <signal.h>")
        write("#include <sys/time.h>")
        write("#include \"apq.h\"")

        write("#define MAX_POLL_COUNT 32")

        // Get streams on this host.
        val localStreams = ap.streams.filter { s =>
            shouldEmit(s.sourceKernel.device) || shouldEmit(s.destKernel.device)
        }

        // Create edge generators for local streams.
        localStreams.foreach { s => addEdgeGenerator(s) }

        // Determine if we need TimeTrial.
        // Note that we need to check every stream on this host.
        val needTimeTrial = ap.streams.filter { s =>
            s.sourceKernel.device.host == host ||
            s.destKernel.device.host == host
        }.exists { s => !s.measures.isEmpty }

        if (needTimeTrial) {
            write("#include \"tta.h\"")
        }

        if (needTimeTrial) {
            val threadCount = ap.threadCount
            val bufferSize = ap.parameters.get[Int]('timeTrialBufferSize)
            write("static XTTASharedMemory tta(" + threadCount + ", " +
                    bufferSize + ");")
            write("static XTTAThread *tta_thread = NULL;")
        }

        write("static volatile bool stopped = false;")
        write("static bool show_extra_stats = false;")

        // Generate code using the edge generators.
        val edgeTop = new ListBuffer[Generator]
        val edgeGlobals = new ListBuffer[Generator]
        val edgeInit = new ListBuffer[Generator]
        val edgeDestroy = new ListBuffer[Generator]
        val edgeStats = new ListBuffer[Generator]
        for (i <- edgeGenerators) {
            val generator = i._1
            val edgeStreams = i._2

            generator.emitCommon()
            edgeTop += generator.extract()

            generator.emitGlobals(edgeStreams)
            edgeGlobals += generator.extract()

            generator.emitInit(edgeStreams)
            edgeInit += generator.extract()

            generator.emitDestroy(edgeStreams)
            edgeDestroy += generator.extract()

            generator.emitStats(edgeStreams)
            edgeStats += generator.extract()

        }

        // Include kernel headers.
        write("extern \"C\" {")
        cpuKernels.foreach { emitKernelHeader(_) }
        write("}")

        // Write the top edge code.
        write(edgeTop)

        // Create kernel structures.
        cpuKernels.foreach { emitKernelStruct(_) }

        write("static unsigned long long start_ticks;")
        write("static struct timeval start_time;")

        // Write the edge globals.
        write(edgeGlobals)

        writeShutdown(cpuKernels, edgeStats)

        // Write the kernel functions.
        cpuKernels.foreach { emitKernelGetFree(_) }
        cpuKernels.foreach { emitKernelAllocate(_) }
        cpuKernels.foreach { emitKernelSendSignal(_) }
        cpuKernels.foreach { emitKernelSend(_) }
        cpuKernels.foreach { emitKernelRelease(_) }
        cpuKernels.foreach { emitCheckRunning(_) }
        cpuKernels.foreach { emitThread(_) }

        // Create the main function.
        write("int main(int argc, char **argv)")
        write("{")
        enter

        // Declare threads.
        for (t <- threadIds.values) {
            write("pthread_t thread" + t + ";")
        }

        write("start_ticks = xrdtsc();")
        write("gettimeofday(&start_time, NULL);")

        write("signal(SIGINT, shutdown);")

        // Startup TimeTrial.
        if (needTimeTrial) {

            val ttOutput = ap.parameters.get[String]('timeTrialOutput)
            val ttFile = if (ttOutput == null) "NULL" else ttOutput
            val ttAffinity = ap.parameters.get[Int]('timeTrialAffinity)
            write("tta_thread = new XTTAThread(&tta, " + ttFile + ", " +
                    ttAffinity + ");")

            val sl = localStreams.filter { s =>
                shouldEmit(s.sourceKernel.device) &&
                shouldEmit(s.destKernel.device)
            }
            for (measure <- sl.flatMap { s => s.measures }) {

                // Note that we can use resource 0 here since
                // no threads have been started yet.
                write("tta.SendStart(0, " + measure.stream.index + ", " +
                        measure.getTTAStat + ", " +
                        measure.getTTAMetric + ", " +
                        measure.stream.depth + ", " +
                        "\"" + measure.getName + "\");")

            }

        }

        // Initialize the edges.
        write(edgeInit)

        // Call the kernel init functions.
        cpuKernels.foreach { emitKernelInit(_) }

        write("atexit(showStats);")

        // Start the threads.
        for (t <- threadIds.values) {
            write("pthread_create(&thread" + t + ", NULL, run_thread" + t +
                    ", NULL);")
        }
        for (t <- threadIds.values) {
            write("pthread_join(thread" + t + ", NULL);")
        }

        // Call the kernel destroy functions.
        cpuKernels.foreach { emitKernelDestroy(_) }

        // Destroy the edges.
        write(edgeDestroy)

        // Shutdown TimeTrial.
        if (needTimeTrial) {
            write("delete tta_thread;")
        }

        write("return 0;")
        leave
        write("}")

        writeFile(dir, "proc_" + host + ".cpp")

    }

}

