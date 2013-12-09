
package autopipe

import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import java.io.File

import autopipe.dsl._
import autopipe.gen.XGenerator
import autopipe.gen.MakefileGenerator
import autopipe.gen.RawFileGenerator

private[autopipe] class AutoPipe {

   private[autopipe] val blocks = new ListBuffer[Block]
   private[autopipe] val streams = new HashSet[Stream]
   private[autopipe] val devices = new HashSet[Device]
   private[autopipe] val parameters = new Parameters
   private val blockTypes = new HashMap[String, HashSet[BlockType]]
   private val functionTypes = new HashMap[String, HashSet[FunctionType]]
   private val edges = new HashSet[EdgeMapping]
   private val measures = new HashSet[EdgeMeasurement]
   private val deviceManager = new DeviceManager(parameters)
   private val resourceManager = new ResourceManager(this)
   private val runHooks = new ListBuffer[()=>Unit]

   private[autopipe] def functions: Traversable[FunctionType] = {
      functionTypes.values.flatten
   }

   private def addBlockType(b: AutoPipeBlock) {

      if (!blockTypes.contains(b.name)) {

         // Create external block types.
         b.externals.foreach { p =>
            val hs = blockTypes.getOrElseUpdate(b.name, new HashSet[BlockType])
            hs += new ExternalBlockType(this, b, p)
         }

         // Create internal block types.
         Platforms.values.filter(p => !b.externals.contains(p)).foreach { p =>
            val hs = blockTypes.getOrElseUpdate(b.name, new HashSet[BlockType])
            hs += new InternalBlockType(this, b, p)
         }

      }

   }

   private def addFunctionType(f: AutoPipeFunction, p: Platforms.Value) {
      val hs = functionTypes.getOrElseUpdate(f.name, new HashSet[FunctionType])
      if (!hs.exists(_.platform == p)) {
         if (f.externals.contains(p)) {
            hs += new ExternalFunctionType(this, f, p)
         } else {
            hs += new InternalFunctionType(this, f, p)
         }
      }
   }

   // Add an edge block at the specified edge(s).
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

   // Determine the number of output ports for a block.
   private[autopipe] def getOutputCount(n: String): Int =
      blockTypes.get(n) match {
         case Some(l) => l.head.outputs.size
         case None => Error.raise("no block with name " + n); 0
      }

   private[autopipe] def createBlock(apb: AutoPipeBlock): Block = {
      val block = new Block(this, apb.name)
      blocks += block

      addBlockType(apb)
      val bts = blockTypes(apb.name)
      if (bts.size == 1) {
         block.blockType = bts.head
      }

      block
   }

   private[autopipe] def getBlockTypes(): Seq[BlockType] = {
      val btl = new ListBuffer[BlockType]
      for (l <- blockTypes) {
         btl ++= l._2
      }
      btl
   }

   private[autopipe] def threadCount: Int = deviceManager.threadCount

   private def insertEdges() {

      for (e <- edges) {
         val fromBlock = e.fromBlock.name
         val toBlock = e.toBlock.name
         for (b <- blocks) {
            for (s <- b.getOutputs) {
               val sname = s.sourceBlock.name
               val dname = s.destBlock.name
               if ((sname == fromBlock || fromBlock == "any") &&
                   (dname == toBlock || toBlock == "any")) {
                  s.setEdge(e.edge)
               }
            }
         }
      }

   }

   private def insertMeasures() {

      // Insert measures specified using edge aspects.
      for (m <- measures) {
         val fromBlock = m.fromBlock.name
         val toBlock = m.toBlock.name
         val stat = m.stat
         val metric = m.metric
         for (b <- blocks) {
            for (s <- b.getOutputs) {
               val sname = s.sourceBlock.name
               val dname = s.destBlock.name
               if ((sname == fromBlock || fromBlock == "any") &&
                   (dname == toBlock || toBlock == "any")) {
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
      val deviceMap = new HashMap[Device, Array[Int]]
      def getIndexArray(d: Device): Array[Int] = {
         if (deviceMap.contains(d)) {
            deviceMap(d)
         } else {
            val a = Array(0, 0, 0, 0)
            deviceMap += ((d, a))
            a
         }
      }

      // Assign activity monitor IDs (index "1" in the array).
      for (m <- ml.filter { !_.useQueueMonitor }) {
         val source = m.stream.sourceBlock.device
         val dest   = m.stream.destBlock.device
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
         val source = m.stream.sourceBlock.device
         val dest   = m.stream.destBlock.device
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
         val source = m.stream.sourceBlock.device
         val dest   = m.stream.destBlock.device
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

   private def assignDevices {

      // Get a list of strongly connected blocks.
      def getConnectedBlocks(block: Block): Seq[Block] = {

         val connected = new HashSet[Block]

         def visit(b: Block) {
            b.getOutputs.filter(_.edge == null).foreach { o =>
               val dest = o.destBlock
               if(!connected.contains(dest)) {
                  connected += dest
                  visit(dest)
               }
            }
            b.getInputs.filter(_.edge == null).foreach { i =>
               val src = i.sourceBlock
               if (!connected.contains(src)) {
                  connected += src
                  visit(src)
               }
            }
         }

         connected += block
         visit(block)
         connected.toList

      }

      // Determine the device to use for a list of connected blocks.
      def getDevice(bl: Seq[Block]): Device = {


         // Check if any of the blocks already have a device assigned.
         for (b <- bl) {
            if (b.device != null) {
               return b.device
            }
         }

         // Look for an incoming edge.
         val inputs = bl.flatMap(_.getInputs)
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
         val outputs = bl.flatMap(_.getOutputs)
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

         // No edges in either direction.
         // Get a list of potential block platforms.
         val platformSet = new HashSet[Platforms.Value]
         for (bt <- blockTypes(bl.head.name)) {
            platformSet += bt.platform
         }
         for (b <- bl) {
            val pl = blockTypes(b.name).map { _.platform }
            platformSet.retain { pl.contains(_) }
         }

         // We need exactly one platform to match.
         if (platformSet.size > 1) {
            Error.warn("multiple device assignments possible; using default")
            if (platformSet.contains(Platforms.C)) {
               platformSet.clear
               platformSet += Platforms.C
            } else {
               val t = platformSet.head
               platformSet.clear
               platformSet += t
            }
         }
         if (platformSet.size == 0) {
            Error.raise("device assignment error: " +
                        "no device assignments possible")
         }

         // Create the default device.
         return deviceManager.getDefault(platformSet.head)

      }

      // Loop over each block to assign devices.
      for (b <- blocks) {

         // Get a list of blocks that are strongly connected to this block.
         val connected = getConnectedBlocks(b)

         // Determine the device to use for the blocks.
         val device = getDevice(connected)

         // Assign the blocks to the device.
         for (x <- connected) {
            if (x.device == null) {
               x.device = device
               device.addBlock(x)
               devices += device
            } else if (x.device != device) {
               Error.raise("device assignment error: " +
                           "blocks assignd to conflicting devices")
            }
         }

      }

   }

   private def loadBlockTypes {
      for (b <- blocks) {
         val platform = b.device.platform
         val bt = blockTypes(b.name).filter(_.platform == platform)
         bt.size match {
            case 0 => Error.raise("block " + b.name + " not available on " +
                                  platform)
            case 1 => b.blockType = bt.head
                     b.blockType.addBlock(b)
            case _ => Error.raise("multiple implementations for block " +
                                  b.name + " on " + platform)
         }
      }
   }

   private def loadStreams {
      streams ++= blocks.flatMap(b => b.blockType.streams)
   }

   private def checkTypes {
      streams.foreach { _.checkType }
   }

   private def emitBlocks(dir: File) {
      blockTypes.values.flatten.filter(t => !t.blocks.isEmpty).foreach { t =>
         t.emit(dir)
      }
   }

   private def emitFunctions(dir: File) {
      blockTypes.values.flatten.filter(t => !t.blocks.isEmpty).foreach { t =>
         t.functions.foreach(f => addFunctionType(f, t.platform))
      }
      functions.foreach(t => t.emit(dir))
   }

   private def emitDescription(dir: File) {
      XGenerator.emit(this)
      XGenerator.writeFile(dir, "map.x")
   }

   private[autopipe] def getRules: String = {
      import scala.collection.immutable.HashSet
      val gens = HashSet(devices.toList.map(d => resourceManager.get(d)): _*)
      gens.foldLeft("") { (a, g) => a ++ g.getRules }
   }

   private def emitResources(dir: File) {
      import scala.collection.immutable.HashSet
      val gens = HashSet(devices.toList.map(d => resourceManager.get(d)): _*)
      gens.foreach(g => g.emit(dir))
   }

   private def emitMakefile(dir: File) {
      val generator = new MakefileGenerator
      generator.emit(this)
      generator.writeFile(dir, "Makefile")
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
      deviceManager.reassignIndexes
      loadBlockTypes
      loadStreams
      checkTypes
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
      emitBlocks(dir)
      emitFunctions(dir)
      emitDescription(dir)
      emitResources(dir)
      emitMakefile(dir)

   }

   private[autopipe] def build(dirname: String) {

      import java.lang.Process
      import java.lang.ProcessBuilder

      emit(dirname)

      val pb = new ProcessBuilder("make")
      pb.directory(new File(dirname))
      val proc = pb.start()
      proc.waitFor()

      if (proc.exitValue() != 0) {
         throw new Exception("Build failed")
      }

   }

   private[autopipe] def run(dirname: String): java.lang.Process = {

      import java.lang.ProcessBuilder

      build(dirname)

      for (f <- runHooks) {
         f()
      }

      // FIXME: The process could be named something different.
      val pb = new ProcessBuilder("./proc_localhost")
      pb.directory(new File(dirname))
      pb.start()

   }

   private[autopipe] def addRunHook(f: () => Unit) {
      runHooks += f
   }

}

