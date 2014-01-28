package autopipe

import java.io.File

import autopipe.dsl._
import autopipe.gen.XGenerator
import autopipe.gen.MakefileGenerator
import autopipe.gen.RawFileGenerator

private[autopipe] class AutoPipe {

    private[autopipe] var kernels = Seq[KernelInstance]()
    private[autopipe] var streams = Set[Stream]()
    private[autopipe] var devices = Set[Device]()
    private[autopipe] val parameters = new Parameters
    private var kernelDecls = Map[String, AutoPipeBlock]()
    private var kernelTypes = Map[(String, Platforms.Value), KernelType]()
    private var edges = Set[EdgeMapping]()
    private var measures = Set[EdgeMeasurement]()
    private val deviceManager = new DeviceManager(parameters)
    private val resourceManager = new ResourceManager(this)

    private def addKernelType(b: AutoPipeBlock,
                              p: Platforms.Value): KernelType = {

        // Check if this kernel already exists for the specified platform.
        val key = (b.name, p)
        if (kernelTypes.contains(key)) {
            return kernelTypes(key)
        }

        // Create a new instance for the specified platform.
        val kt = b match {
            case f: AutoPipeFunction =>
                if (b.externals.contains(p)) {
                    new ExternalFunctionType(this, f, p)
                } else {
                    new InternalFunctionType(this, f, p)
                }
            case _ =>
                if (b.externals.contains(p)) {
                    new ExternalKernelType(this, b, p)
                } else {
                    new InternalKernelType(this, b, p)
                }
        }

        // Add the kernel to our map.
        kernelTypes = kernelTypes + (key -> kt)

        return kt

    }

    // Add an edge kernel at the specified edge(s).
    private[autopipe] def addEdge(edge: EdgeMapping) {
        edges += edge
    }

    // Add a measurement at the specified edge(s).
    private[autopipe] def addMeasure(measure: EdgeMeasurement) {
        measures += measure
    }

    // Set a parameter.
    private[autopipe] def setParameter(param: Symbol, value: Any) {
        parameters.set(param, value)
    }

    // Determine the number of output ports for a kernel.
    private[autopipe] def getOutputCount(n: String): Int =
        kernelDecls.get(n) match {
            case Some(kd)   => kd.outputs.size
            case None       => Error.raise("no kernel with name " + n); 0
        }

    private[autopipe] def createKernel(apb: AutoPipeBlock): KernelInstance = {
        kernelDecls.get(apb.name) match {
            case Some(kd) =>
                if (kd != apb) {
                    Error.raise("multiple kernels with name " + apb.name)
                }
            case None =>
                kernelDecls = kernelDecls + (apb.name -> apb)
        }
        val kernel = new KernelInstance(this, apb)
        kernels = kernels :+ kernel
        kernel
    }

    private[autopipe] def createStream(sourceKernel: KernelInstance,
                                       sourcePort: PortName): Stream = {
        val stream = new Stream(this, sourceKernel, sourcePort)
        streams = streams + stream
        stream
    }

    private[autopipe] def getKernelTypes(d: Device = null) = {
        val ks = if (d != null) kernels.filter(_.device == d) else kernels
        val kts = ks.map { k =>
            addKernelType(k.apb, k.device.platform)
        }
        val funcs = kts.flatMap { kt =>
            kt.functions.map(f => addKernelType(f, kt.platform))
        }
        kts.toSet ++ funcs
    }

    private[autopipe] def kernelType(name: String, p: Platforms.Value) =
        kernelTypes((name, p))

    private[autopipe] def threadCount: Int = deviceManager.threadCount

    private def insertEdges {

        for (e <- edges) {
            val fromKernel = e.fromKernel.name
            val toKernel = e.toKernel.name
            for (k <- kernels) {
                for (s <- k.getOutputs) {
                    val sname = s.sourceKernel.name
                    val dname = s.destKernel.name
                    if ((sname == fromKernel || fromKernel == "any") &&
                         (dname == toKernel || toKernel == "any")) {
                        s.setEdge(e.edge)
                    }
                }
            }
        }

    }

    private def insertMeasures {

        // Insert measures specified using edge aspects.
        for (m <- measures) {
            val fromKernel = m.fromKernel.name
            val toKernel = m.toKernel.name
            val stat = m.stat
            val metric = m.metric
            for (k <- kernels) {
                for (s <- k.getOutputs) {
                    val sname = s.sourceKernel.name
                    val dname = s.destKernel.name
                    if ((sname == fromKernel || fromKernel == "any") &&
                         (dname == toKernel || toKernel == "any")) {
                        s.addMeasure(stat, metric)
                    }
                }
            }
        }

        // Get a list of Measure objects.
        val ml = streams.flatMap { _.measures }

        // Assign indexes.
        // The hardware monitor has three types of monitors, each of which
        // requires a unique ID starting from 0.  Further, there is a global
        // ID starting from 0.
        // Activity monitors come first, then queue monitors, and finally,
        // inter monitors.
        var deviceMap = Map[Device, Array[Int]]()
        def getIndexArray(d: Device): Array[Int] = {
            if (deviceMap.contains(d)) {
                deviceMap(d)
            } else {
                val a = Array(0, 0, 0, 0)
                deviceMap = deviceMap + (d -> a)
                a
            }
        }

        // Assign activity monitor IDs (index "1" in the array).
        for (m <- ml.filter { !_.useQueueMonitor }) {
            val source = m.stream.sourceKernel.device
            val dest    = m.stream.destKernel.device
            val sourceArray = getIndexArray(source)
            val destArray = getIndexArray(dest)
            m.setSourceOffsets(sourceArray)
            m.setDestOffsets(destArray)

            // Update the source device information.
            if (m.useInputActivity) {
                sourceArray(0) += 1
                sourceArray(1) += 1
            }
            if (m.useOutputActivity) {
                sourceArray(0) += 1
                sourceArray(1) += 1
            }
            if (m.useFullActivity) {
                sourceArray(0) += 1
                sourceArray(1) += 1
            }

            // Update the destination device information.
            if (source != dest) {
                if (m.useInputActivity) {
                    sourceArray(0) += 1
                    sourceArray(1) += 1
                }
                if (m.useOutputActivity) {
                    sourceArray(0) += 1
                    sourceArray(1) += 1
                }
                if (m.useFullActivity) {
                    sourceArray(0) += 1
                    sourceArray(1) += 1
                }
            } else {
                destArray(0) = sourceArray(0)
                destArray(1) = sourceArray(1)
            }

        }

        // Assign queue monitor IDs (index "2" in the array).
        for (m <- ml) {
            val source = m.stream.sourceKernel.device
            val dest    = m.stream.destKernel.device
            val sourceArray = getIndexArray(source)
            val destArray = getIndexArray(dest)

            // Update the source device information.
            if (m.useQueueMonitor) {
                m.setSourceOffsets(sourceArray)
                m.setDestOffsets(destArray)
                sourceArray(0) += 1
                sourceArray(2) += 1
            }

            // Update the destination device information.
            if (source != dest) {
                if (m.useQueueMonitor) {
                    sourceArray(0) += 1
                    sourceArray(2) += 1
                }
            } else {
                destArray(0) = sourceArray(0)
                destArray(2) = sourceArray(2)
            }

        }

        // Assign inter monitor IDs (index "3" in the array).
        for (m <- ml) {
            val source = m.stream.sourceKernel.device
            val dest    = m.stream.destKernel.device
            val sourceArray = getIndexArray(source)
            val destArray = getIndexArray(dest)

            // Update the source device information.
            if (m.useInterPush || m.useInterPop) {
                m.setSourceOffsets(sourceArray)
                m.setDestOffsets(destArray)
                sourceArray(0) += 1
                sourceArray(3) += 1
            }

            // Update the destination device information.
            if (source != dest) {
                if (m.useInterPush || m.useInterPop) {
                    sourceArray(0) += 1
                    sourceArray(3) += 1
                }
            } else {
                destArray(0) = sourceArray(0)
                destArray(3) = sourceArray(3)
            }

        }

    }

    // Get a list of strongly connected kernels.
    private def getConnectedKernels(kernel: KernelInstance) = {

        var connected = Set[KernelInstance]()

        def visit(k: KernelInstance) {
            k.getOutputs.filter(_.edge == null).foreach { o =>
                val dest = o.destKernel
                if(!connected.contains(dest)) {
                    connected = connected + dest
                    visit(dest)
                }
            }
            k.getInputs.filter(_.edge == null).foreach { i =>
                val src = i.sourceKernel
                if (!connected.contains(src)) {
                    connected = connected + src
                    visit(src)
                }
            }
        }

        connected = connected + kernel
        visit(kernel)
        connected.toSeq

    }

    // Determine the device to use for a list of connected kernels.
    private def getDevice(kl: Seq[KernelInstance]): Device = {


        // Check if any of the kernels already have a device assigned.
        for (k <- kl if k.device != null) {
            return k.device
        }

        // Look for an incoming edge.
        val inputs = kl.flatMap(_.getInputs)
        val inEdges = inputs.filter(_.edge != null).map(_.edge)
        if (!inEdges.isEmpty) {

            // We have one or more incoming edges.
            // Get the device from the edge and make sure all
            // of the devices match.
            var spec: DeviceSpec = null
            for (e <- inEdges) {
                if (spec != null && !spec.canCombine(e.dest)) {
                    Error.raise("device assignment error: " +
                                "in-edges from multiple devices (" +
                                e.dest + " and " + spec + ")")
                }
                spec = e.dest.combine(spec)
            }

            return deviceManager.create(spec)

        }

        // No incoming edges.
        // Check output edges.
        val outputs = kl.flatMap(_.getOutputs)
        val outEdges = outputs.filter(_.edge != null).map(_.edge)
        if (!outEdges.isEmpty) {

            // Get the default device and make sure they all match.
            var spec: DeviceSpec = null
            for (e <- outEdges) {
                val source = e.defaultSource
                if (spec != null && !spec.canCombine(source)) {
                    Error.raise("device assignment error: " +
                                "out-edges to multiple devices (" +
                                source + " and " + spec + ")")
                }
                spec = source.combine(spec)
            }

            return deviceManager.create(spec)

        }

        // No edges in either direction; assume the default.
        return deviceManager.getDefault(Platforms.C)

    }

    private def assignDevices {

        // Loop over each kernel to assign devices.
        for (k <- kernels) {

            // Get a list of kernels that are strongly connected to
            // this kernel.
            val connected = getConnectedKernels(k)

            // Determine the device to use for the kernels.
            val device = getDevice(connected)

            // Assign the kernels to the device.
            for (x <- connected) {
                if (x.device == null) {
                    x.device = device
                    devices += device
                } else if (x.device != device) {
                    Error.raise("device assignment error: " +
                                "kernels assignd to conflicting devices")
                }
            }
        }
    }

    private def createKernelTypes {
        val kts = kernels.map { k =>
            addKernelType(k.apb, k.device.platform)
        }.distinct
        kts.foreach { kt =>
            kt.functions.foreach { f =>
                addKernelType(f, kt.platform)
            }
        }
    }

    private def checkStreams {
        streams.foreach { _.checkType }
    }

    private def checkKernels {
        kernels.foreach { _.validate }
    }

    private def emitKernels(dir: File) {
        kernelTypes.values.foreach { kt =>
            kt.emit(dir)
        }
    }

    private def emitDescription(dir: File) {
        XGenerator.emit(this)
        XGenerator.writeFile(dir, "map.x")
    }

    private[autopipe] def getRules: String = {
        val gens = devices.map(d => resourceManager.get(d))
        gens.map(_.getRules).mkString
    }

    private def emitResources(dir: File) {
        val gens = devices.map(d => resourceManager.get(d))
        gens.foreach(g => g.emit(dir))
    }

    private def emitMakefile(dir: File) {
        val generator = new MakefileGenerator(this)
        generator.emit(dir)
    }

    private def emitTimeTrial(dir: File) {

        val hasTimeTrial = !streams.filter { !_.measures.isEmpty }.isEmpty

        if (hasTimeTrial) {
            RawFileGenerator.emitFile(dir, "tta.h")
            RawFileGenerator.emitFile(dir, "tta.cpp")
            RawFileGenerator.emitFile(dir, "Measure.hh")
            RawFileGenerator.emitFile(dir, "Stat.hh")
        }

    }

    private[autopipe] def emit(dirname: String) {

        insertEdges
        assignDevices
        createKernelTypes
        deviceManager.reassignIndexes
        checkStreams
        checkKernels
        insertMeasures

        // Create the directory.
        val dir = new File(dirname)
        dir.mkdir

        RawFileGenerator.emitFile(dir, "apq.h")
        RawFileGenerator.emitFile(dir, "X.h")
        RawFileGenerator.emitFile(dir, "fp.v")

        val fpga = parameters.get[String]('fpga)
        fpga match {
            case "SmartFusion" =>
                RawFileGenerator.emitFile(dir, "smartfusion.v")
            case "Simulation" =>
                RawFileGenerator.emitFile(dir, "int.v")
            case _ => Error.raise("Unknown FPGA type: " + fpga)
        }

        emitTimeTrial(dir)
        emitKernels(dir)
        emitDescription(dir)
        emitResources(dir)
        emitMakefile(dir)
        Error.exit

    }

}
