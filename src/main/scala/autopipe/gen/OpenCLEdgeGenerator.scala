
package autopipe.gen

import scala.collection.mutable.HashSet

import autopipe._

private[autopipe] class OpenCLEdgeGenerator(val ap: AutoPipe)
   extends EdgeGenerator(Platforms.OpenCL) {

   override def emitCommon() {

      write("#ifdef __APPLE__")
      write("#include <OpenCL/opencl.h>")
      write("#else")
      write("#include <CL/opencl.h>")
      write("#endif")
      write("#include <semaphore.h>")
      write("#include <string.h>")
      write
      write("#define OCL_STATE_READY         0")
      write("#define OCL_STATE_RUNNING       1")
      write("#define OCL_STATE_FINISHED      2")
      write("#define OCL_STATE_TRANSFERRING  3")
      write
      write("typedef struct {")
      enter
      write("APQ *queue;")
      write("cl_mem handle;")
      write("cl_event event;")
      write("uint32_t copied;")
      write("uint32_t offset;")
      leave
      write("} OCLPort;")
      write
      write("typedef struct {")
      enter
      write("int state;")
      write("cl_kernel kernel;")
      write("cl_command_queue queue;")
      write("cl_mem control_handle;")
      write("cl_mem data_handle;")
      write("cl_event *events;")
      write("int event_count;")
      write("APC clock;")
      leave
      write("} OCLBlock;")

   }

   private def writeTypes(types: HashSet[ValueType]) {
      for (t <- types) {
         write("#ifndef DECLARED_" + t.name)
         write("#define DECLARED_" + t.name)
         t match {
            case at: ArrayValueType =>
               write("typedef struct {")
               write(at.itemType + " values[" + at.length + "];")
               write("} " + at.name + ";")
            case td: TypeDefValueType =>
               write("typedef " + td.value + " " + td.name + ";")
            case ft: FixedValueType =>
               write("typedef " + ft.baseType + " " + ft.name + ";")
            case _ =>
               write("typedef " + t + " " + t.baseType.name + ";")
         }
         write("#endif")
      }
   }

   override def emitGlobals(streams: Traversable[Stream]) {

      val devices = getDevices(streams)

      for (device <- devices) {

         write("static pthread_t " + device.label + "_tid;")
         write("static sem_t " + device.label + "_sem;")
         write("static cl_context " + device.label + "_context;")
         write("static size_t " + device.label + "_wgsize;")

         // Edges into this device.
         for (s <- getSenderStreams(device, streams)) {
            write("static OCLPort " + s.label + "_data;")
            writeSendFunctions(s)
         }

         // Edges out of this device.
         for (s <- getReceiverStreams(device, streams)) {
            write("static OCLPort " + s.label + "_data;")
            writeReceiveFunctions(s)
         }

         // Edges internal to this device.
         // Note that we need to use ap.streams since streams
         // won't contain these edges.
         for (s <- getInternalStreams(device, ap.streams)) {
            write("static OCLPort " + s.label + "_data;")
         }

         // Get blocks on this device.
         val localBlocks = getBlocks(device, ap.blocks)

         // Get unique block types for this device.
         val blockTypes = localBlocks.map { _.blockType }.toList.distinct

         // Declare kernels for each block type.
         for (bt <- blockTypes) {
            val kernel = device.label + "_" + bt.name + "_kernel"
            write("static cl_kernel " + kernel + ";")
         }

         // Declare data for blocks on this device.
         write("extern \"C\" {")
         for (bt <- localBlocks.map { _.blockType }.toList.distinct) {
            val name = bt.name
            write("#include \"" + name + "-dir/" + name + ".cl\"")

         }
         write("}")
         for (b <- localBlocks) {

            write("static OCLBlock " + b.label + "_block;")

            write("static struct {")
            enter

            write("cl_int ap_ready;")
            write("cl_int ap_state_index;")

            for (s <- b.blockType.inputs) {
               write("cl_int " + s + "_size;")
               write("cl_int " + s + "_read;")
            }

            for (s <- b.blockType.outputs) {
               write("cl_int " + s + "_size;")
               write("cl_int " + s + "_sent;")
            }

            leave
            write("} " + b.label + "_control;")

            val uniqueTypes = new HashSet[ValueType]
            b.blockType.states.foreach { s => uniqueTypes += s.valueType }
            writeTypes(uniqueTypes)

            write("static struct {")
            enter

            // Write state variables for internal blocks.
            if (b.blockType.internal) {
               for (s <- b.blockType.states if !s.isLocal) {
                  val valueType = s.valueType
                  val name = s.name
                  write(valueType.name + " " + name + ";")
               }
            }

            leave
            write("} " + b.label + "_data;")

         }

         writeProcessThread(device)

      }

   }

   private def writeSendFunctions(stream: Stream) {

      val queueName = stream.label + "_data.queue"
      val sem = stream.destBlock.device.label + "_sem"

      // "get_free" - Run on the producer thread (source).
      write("static int " + stream.label + "_get_free()")
      write("{")
      enter
      write("return APQ_GetFree(" + queueName + ");")
      leave
      write("}")

      // "allocate" - Run on the producer thread (source).
      write("static void *" + stream.label + "_allocate(int count)")
      write("{")
      enter
      write("return APQ_StartWrite(" + queueName + ", count);")
      leave
      write("}")

      // "send" - Run on the producer thread (source).
      write("static void " + stream.label + "_send(int count)")
      write("{")
      enter
      write("APQ_FinishWrite(" + queueName + ", count);")
//      write("sem_post(&" + sem + ");")
      leave
      write("}")

      // "send_signal" - Run on the producer thread (source).
      write("static void " + stream.label + "_send_signal(UNSIGNED64 s)")
      write("{")
      enter
      leave
      write("}")

   }

   private def writeReceiveFunctions(stream: Stream) {

      val queueName = stream.label + "_data.queue"
      val sem = stream.sourceBlock.device.label + "_sem"
      val destBlock = stream.destBlock
      val destIndex = stream.destIndex

      // "process"
      write("static void " + stream.label + "_process()")
      write("{")
      enter
      write("char *buf = NULL;")
      write("uint32_t c = APQ_StartRead(" + queueName + ", &buf);")
      write("if(c > 0 && " +
            destBlock.label + ".inputs[" + destIndex + "].data == NULL) {")
      enter
      write(destBlock.label + ".inputs[" + destIndex + "].data = buf;")
      write(destBlock.label + ".clock.count += 1;")
      write("APC_Start(&" + destBlock.label + ".clock);")
      write("ap_" + destBlock.blockType.name + "_push(&" + destBlock.label +
            ".priv, " + destIndex + ", " + destBlock.label + ".inputs[" +
            destIndex + "].data, c);")
      write("APC_Stop(&" + destBlock.label + ".clock);")
      leave
      write("}")
      leave
      write("}")

      // "release"
      write("static void " + stream.label + "_release(int count)")
      write("{")
      enter
      write("APQ_FinishRead(" + queueName + ", count);")
//      write("sem_post(&" + sem + ");")
      leave
      write("}")

   }

   private def initSender(device: Device, stream: Stream) {

      val context = device.label + "_context"
      val depth = stream.depth
      val valueType = stream.valueType
      val copied = stream.label + "_data.copied"
      val offset = stream.label + "_data.offset"

      write(stream.label + "_data.queue = (APQ*)malloc(APQ_GetSize(" +
            depth + ", sizeof(" + valueType + ")));")
      write("APQ_Initialize(" + stream.label + "_data.queue, " +
            depth + ", sizeof(" + valueType + "));")
      write(stream.label + "_data.handle = clCreateBuffer(" +
            context + ", CL_MEM_READ_WRITE, " +
            depth + " * sizeof(" + valueType + "), NULL, &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateBuffer failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")
      write(copied + " = 0;")
      write(offset + " = 0;")

   }

   private def initReceiver(device: Device, stream: Stream) {

      val context = device.label + "_context"
      val depth = stream.depth
      val valueType = stream.valueType

      // Note that we cap the max transfer to the queue depth / 2, so
      // we only need to allocate half of the queue depth on the device.

      write(stream.label + "_data.queue = (APQ*)malloc(APQ_GetSize(" +
            depth + ", sizeof(" + valueType + ")));")
      write("APQ_Initialize(" + stream.label + "_data.queue, " +
            depth + ", sizeof(" + valueType + "));")
      write(stream.label + "_data.handle = clCreateBuffer(" +
            context + ", CL_MEM_READ_WRITE, " +
            (depth / 2 ) + " * sizeof(" + valueType + "), NULL, &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateBuffer failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

   }

   private def initInternal(device: Device, stream: Stream) {

      val context = device.label + "_context"
      val depth = stream.depth
      val valueType = stream.valueType

      write(stream.label + "_data.queue = (APQ*)malloc(sizeof(APQ));")
      write("APQ_Initialize(" + stream.label + "_data.queue, " +
            depth + ", sizeof(" + valueType + "));")
      write(stream.label + "_data.handle = clCreateBuffer(" +
            context + ", CL_MEM_READ_WRITE, " +
            depth + " * sizeof(" + valueType + "), NULL, &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateBuffer failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

   }

   private def initKernel(device: Device, device_index: Int, bt: BlockType) {

      val kernel = device.label + "_" + bt.name + "_kernel"
      val devid = "devices[" + device_index + "]"
      val context = device.label + "_context"
      val source = bt.name + "_source"

      // Create the program.
      write("program = clCreateProgramWithSource(" + context + ", 1, " +
            source + ", NULL, &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateProgramWithSource failed: " +
            "%d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

      // Build the program.
      write("rc = clBuildProgram(program, 1, &" + devid +
            ", NULL, NULL, NULL);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("char *buffer;")
      write("size_t bufsize;")
      write("fprintf(stderr, \"clBuildProgram failed: %d\\n\", rc);")
      write("clGetProgramBuildInfo(program, " + devid + ", " +
            "CL_PROGRAM_BUILD_LOG, 0, NULL, &bufsize);")
      write("buffer = new char[bufsize];")
      write("memset(buffer, 0, bufsize);")
      write("clGetProgramBuildInfo(program, " + devid + ", " +
            "CL_PROGRAM_BUILD_LOG, bufsize, buffer, NULL);")
      write("fprintf(stderr, \"%s\", buffer);")
      write("delete [] buffer;")
      write("exit(-1);")
      leave
      write("}")

      // Create the kernel.
      write(kernel + " = clCreateKernel(program, \"" + bt.name + "\", &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateKernel failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

   }

   private def initDevice(device: Device, device_index: Int) {

      val context = device.label + "_context"
      val devid = "devices[" + device_index + "]"

      // Create a context.
      write(context + "= clCreateContext(NULL, 1, &" + devid +
            ", NULL, NULL, &rc);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clCreateContext failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

      // Get the maximum number of items in a workgroup.
      write("rc = clGetDeviceInfo(" + devid +
            ", CL_DEVICE_MAX_WORK_GROUP_SIZE, sizeof(size_t), " +
            "&" + device.label + "_wgsize, NULL);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clGetDeviceInfo failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

      // Get the maximum number of work item dimensions.
      write("cl_uint work_dim = 1;")
      write("rc = clGetDeviceInfo(" + devid +
            ", CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS, sizeof(cl_uint), " +
            "&work_dim, NULL);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clGetDeviceInfo failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")

      // Get the maximum number of items in a single dimension of
      // a workgroup.  Note that we only use a single dimension.
      write("size_t *sizes = new size_t[work_dim];")
      write("rc = clGetDeviceInfo(" + devid +
            ", CL_DEVICE_MAX_WORK_ITEM_SIZES, sizeof(size_t) * work_dim, " +
            "sizes, NULL);")
      write("rc = clGetDeviceInfo(" + devid +
            ", CL_DEVICE_MAX_WORK_ITEM_DIMENSIONS, sizeof(cl_uint), " +
            "&work_dim, NULL);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clGetDeviceInfo failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")
      write(device.label + "_wgsize = sizes[0] < " + device.label + "_wgsize" +
            " ? sizes[0] : " + device.label + "_wgsize;")
      write("delete [] sizes;")
      write("fprintf(stderr, \"Workgroup size for device %d is %lu\\n\", " +
            device_index + ", " + device.label + "_wgsize);")

      // Initialize edges into the GPU.
      for (stream <- getSenderStreams(device, ap.streams)) {
         initSender(device, stream)
      }

      // Initialize edge out of the GPU.
      for (stream <- getReceiverStreams(device, ap.streams)) {
         initReceiver(device, stream)
      }

      // Initialize internal edges.
      for (stream <- getInternalStreams(device, ap.streams)) {
         initInternal(device, stream)
      }

      // Initialize kernels.
      val localBlocks = ap.blocks.filter { b => b.device == device }
      for (bt <- localBlocks.map { _.blockType }.toList.distinct) {
         initKernel(device, device_index, bt)
      }

      // Set up block instances.
      for (block <- localBlocks) {

         val kernel = block.label + "_block.kernel"
         val commandQueue = block.label + "_block.queue"
         val controlHandle = block.label + "_block.control_handle"
         val dataHandle = block.label + "_block.data_handle"
         val state = block.label + "_block.state"
         val clock = block.label + "_block.clock"
         val blockEvents = block.label + "_block.events"
         val control = block.label + "_control"
         val data = block.label + "_data"

         write(kernel + " = " +
               device.label + "_" + block.blockType.name + "_kernel;")

         // Create the command queue.
         write(commandQueue + " = clCreateCommandQueue(" +
               device.label + "_context, " + devid + ", " + "0, &rc);")
         write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clCreateCommandQueue failed: %d\\n\", rc);")
         write("exit(-1);")
         leave
         write("}")

         // Create a handle for the block control data.
         write(controlHandle + " = clCreateBuffer(" + context +
               ", CL_MEM_READ_WRITE, sizeof(" + control + "), NULL, &rc);")
         write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clCreateBuffer failed: %d\\n\", rc);")
         write("exit(-1);")
         leave
         write("}")

         // Create a handle for the local block data.
         // Note that we write this block at startup and then leave it.
         write(dataHandle + " = clCreateBuffer(" + context +
               ", CL_MEM_READ_WRITE, sizeof(" + data + "), NULL, &rc);")
         write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clCreateBuffer failed: %d\\n\", rc);")
         write("exit(-1);")
         leave
         write("}")

         // Initialize the block state.
         write(control + ".ap_state_index = 0;")
         write("APC_Init(&" + clock + ");")
         for (s <- block.blockType.states if !s.isLocal) {
            val name = s.name
            val literal = s.value
            if (literal != null) {
               val str = block.blockType.getLiteral(literal)
               write(data + "." + name + " = " + str + ";")
            }
         }

         // Copy the initial block state to the device.
         write("rc = clEnqueueWriteBuffer(" + commandQueue + ", " +
               dataHandle + ", CL_TRUE, 0, sizeof(" + data + "), " +
               "&" + data + ", 0, NULL, NULL);")
         write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clEnqueueWriteBuffer failed: %d\\n\", rc);")
         write("exit(-1);")
         leave
         write("}")

         // Initialize space for block events.
         // We need one for each device-to-host transfer.
         val outputCount = block.getOutputs.size
         write(blockEvents + " = new cl_event[" + outputCount + "];")

         // Set up pending transfers and state.
         write(state + " = OCL_STATE_READY;")

      }

      // Start the process thread.
      write("sem_init(&" + device.label + "_sem, 0, 0);")
      write("pthread_create(&" + device.label + "_tid, NULL, " +
            device.label + "_thread, NULL);")

   }

   override def emitInit(streams: Traversable[Stream]) {

      val devices = getDevices(streams)
      val deviceCount = devices.size

      write("{")
      enter
      write("cl_platform_id platforms[8];")
      write("cl_device_id devices[" + deviceCount + "];")
      write("cl_uint num = 0;")
      write("cl_int rc;")
      write("cl_program program;")

      // Get a list of platforms.
      // FIXME: This shouldn't just use the first platform...
      write("rc = clGetPlatformIDs(8, platforms, &num);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clGetPlatformIDs failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")
      write("if(XUNLIKELY(num == 0)) {")
      enter
      write("fprintf(stderr, \"No OpenCL platforms found\\n\");")
      write("exit(-1);")
      leave
      write("}")

      // Get a list of device IDs.
      write("rc = clGetDeviceIDs(platforms[0], CL_DEVICE_TYPE_GPU, " +
            deviceCount + ", devices, &num);")
      write("if(XUNLIKELY(rc != CL_SUCCESS)) {")
      enter
      write("fprintf(stderr, \"clGetPlatformIDs failed: %d\\n\", rc);")
      write("exit(-1);")
      leave
      write("}")
      write("if(XUNLIKELY(num < " + deviceCount + ")) {")
      enter
      write("fprintf(stderr, \"Too few OpenCL devices: %u\\n\", num);")
      write("exit(-1);")
      leave
      write("}")

      for (d <- devices.toList.zipWithIndex) {
         val device = d._1
         val device_index = d._2

         initDevice(device, device_index)

      }

      leave
      write("}")

   }

   private def writeProcessThread(device: Device) {

      val sem = device.label + "_sem"

      // Get blocks mapped to this device.
      val localBlocks = ap.blocks.filter { b => b.device == device }

      // Determine the maximum number of host-to-device streams for a block.
      // This is used to allocate enough space in the event array.
      val maxHostDeviceStreams = localBlocks.foldLeft(0) { (a, b) =>
         val t = b.getInputs.count { _.sourceBlock.device != device }
         math.max(a, t)
      }

      // The thread function.
      write("static void *" + device.label + "_thread(void *arg)")
      write("{")
      enter
      write
      write("cl_event events[" + (maxHostDeviceStreams + 1) + "];")
      write("cl_event taskEvent;")
      write("cl_int status;")
      write("char *buf;")
      write("size_t available;")
      write("int pending;")
      write("int buffer_offset = 0;")
      write("bool has_data;")
      write
      write("while(XLIKELY(!stopped)) {")
      enter

      // Start kernels.
      for (block <- localBlocks) {

         val kernel = block.label + "_block.kernel"
         val blockControlHandle = block.label + "_block.control_handle"
         val blockDataHandle = block.label + "_block.data_handle"
         val blockState = block.label + "_block.state"
         val commandQueue = block.label + "_block.queue"
         val clock = block.label + "_block.clock"
         val blockData = block.label + "_data"
         val blockControl = block.label + "_control"
         val readyFlag = blockControl + ".ap_ready"
         val blockEvents = block.label + "_block.events"
         val blockEventCount = block.label + "_block.event_count"
         var arg = 0

         // Check if we should attempt to start this kernel.
         write("APC_Start(&" + clock + ");")
         write("switch(" + blockState + ") {")
         write("case OCL_STATE_READY:")
         enter
         write("pending = 0;")

         // Enqueue input argument copies from host.
         val remoteInputs = block.getInputs.filter {
            _.sourceBlock.device != device
         }
         for (stream <- remoteInputs) {

            val queueName = stream.label + "_data.queue"
            val copied = stream.label + "_data.copied"
            val offset = stream.label + "_data.offset"
            val valueType = stream.valueType
            val index = stream.destIndex
            val sizeval = blockControl + ".input" + index + "_size"
            val readval = blockControl + ".input" + index + "_read"

            write("available = APQ_StartReadOffset(" + queueName +
                  ", &buffer_offset);")
            write("if(buffer_offset == 0 && available > 0) {")
            enter
            write(copied + " = 0;")
            leave
            write("}")
            write(sizeval + " = buffer_offset + available;")
            write(readval + " = buffer_offset;")
            write(offset + " = buffer_offset;")
            write("if(buffer_offset + available > " + copied + ") {")
            enter
            write("size_t offset = " + copied + " * sizeof(" + valueType + ");")
            write("size_t to_copy = buffer_offset + available - " +
                  copied + ";")
            write("to_copy *= sizeof(" + valueType + ");")
            write("buf = &" + queueName + "->data[offset];")
            write("status = clEnqueueWriteBuffer(" + commandQueue + ", " +
                  stream.label + "_data.handle, CL_FALSE, offset, to_copy, " +
                  "buf, 0, NULL, &events[pending]);")
            write("if(XUNLIKELY(status != CL_SUCCESS)) {")
            enter
            write("fprintf(stderr, \"clEnqueueWriteBuffer failed: %d\\n\", " +
                  " status);")
            write("exit(-1);")
            leave
            write("}")
            write("pending += 1;")
            write(copied + " = buffer_offset + available;")
            leave
            write("}")

         }

         // Set up local inputs.
         val localInputs = block.getInputs.filter {
            _.sourceBlock.device == device
         }
         for (stream <- localInputs) {

            val queueName = stream.label + "_data.queue"
            val offset = stream.label + "_data.offset"
            val valueType = stream.valueType
            val index = stream.destIndex
            val sizeval = blockControl + ".input" + index + "_size"
            val readval = blockControl + ".input" + index + "_read"

            write("buffer_offset = 0;")
            write("available = APQ_StartReadOffset(" + queueName +
                  ", &buffer_offset);")
            write(sizeval + " = buffer_offset + available;")
            write(readval + " = buffer_offset;")
            write(offset + " = buffer_offset;")

         }

         // Determine if we should attempt to run again.
         write("has_data = " + blockControl + ".ap_state_index >= 0;")
         for (stream <- block.getInputs) {
            val index = stream.destIndex
            val sizeval = blockControl + ".input" + index + "_size"
            val readval = blockControl + ".input" + index + "_read"
            write("has_data = has_data || " + readval + " < " + sizeval + ";")
         }

         // Setup out-bound buffer sizes.
         for (stream <- block.getOutputs) {
            val index = stream.sourceIndex
            val sizeval = blockControl + ".output" + index + "_size"
            val sentval = blockControl + ".output" + index + "_sent"
            val queueName = stream.label + "_data.queue"
            val copied = stream.label + "_data.copied"
            write("if(has_data) {")
            enter
            if (stream.destBlock.device == device) {
               write(sentval + " = APQ_StartWriteOffset(" + queueName +
                     ", " + queueName + "->depth / 4);")
               write("if(" + sentval + " >= 0) {")
               enter
               write(sizeval + " = " + sentval + " + " +
                     queueName + "->depth / 4;")
               leave
               write("} else {")
               enter
               write(sentval + " = 0;")
               write(sizeval + " = 0;")
               leave
               write("}")
               write(copied + " = " + sentval + ";")
            } else {
               write(sizeval + " = " + queueName + "->depth / 2;")
               write(sentval + " = 0;")
               write(copied + " = 0;")
            }
            write("has_data = " + sentval + " < " + sizeval + ";")
            leave
            write("}")
         }

         write("if(has_data) {")
         enter

         // Mark that the data is not ready.
         // The kernel should set this flag so we know when the block is
         // transfered back.
         write(readyFlag + " = 0;")

         // Set up block-local data.
         write("status = clSetKernelArg(" + kernel + ", " + arg +
               ", sizeof(cl_mem), &" + blockControlHandle + ");")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clSetKernelArg failed: %d\\n\", status);")
         write("exit(-1);")
         leave
         write("}")
         arg += 1
         write("status = clSetKernelArg(" + kernel + ", " + arg +
               ", sizeof(cl_mem), &" + blockDataHandle + ");")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clSetKernelArg failed: %d\\n\", status);")
         write("exit(-1);")
         leave
         write("}")
         arg += 1

         // Set up the inputs.
         for (stream <- block.getInputs) {
            write("status = clSetKernelArg(" + kernel + ", " + arg +
                  ", sizeof(cl_mem), &" + stream.label + "_data.handle);")
            write("if(XUNLIKELY(status != CL_SUCCESS)) {")
            enter
            write("fprintf(stderr, \"clSetKernelArg failed: %d\\n\", status);")
            write("exit(-1);")
            leave
            write("}")
            arg += 1
         }

         // Set up the outputs.
         for (stream <- block.getOutputs) {
            write("status = clSetKernelArg(" + kernel + ", " + arg +
                  ", sizeof(cl_mem), &" + stream.label + "_data.handle);")
            write("if(XUNLIKELY(status != CL_SUCCESS)) {")
            enter
            write("fprintf(stderr, \"clSetKernelArg failed: %d\\n\", status);")
            write("exit(-1);")
            leave
            write("}")
            arg += 1
         }

         // Enqueue control data copy (host-to-device).
         write("status = clEnqueueWriteBuffer(" + commandQueue + ", " +
               blockControlHandle + ", CL_FALSE, 0, " +
               "sizeof(" + blockControl + "), " +
               "&" + blockControl + ", 0, NULL, &events[pending]);")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clEnqueueWriteBuffer failed: %d\\n\", " +
               "status);")
         write("exit(-1);")
         leave
         write("}")

         // Enqueue the kernel.
         if (block.blockType.states.filter { s => !s.isLocal }.isEmpty) {
            write("size_t workgroup_size = " + device.label + "_wgsize;")
            for (o <- block.getOutputs) {
               val maxdepth = o.depth / 4
               write("if(workgroup_size > " + maxdepth + ") {")
               enter
               write("workgroup_size = " + maxdepth + ";")
               leave
               write("}")
            }
         } else {
            write("const size_t workgroup_size = 1;")
         }
         write("status = clEnqueueNDRangeKernel(" + commandQueue + ", " +
               kernel + ", 1, NULL, &workgroup_size, &workgroup_size, " +
               "pending + 1, events, &taskEvent);")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clEnqueueNDRangeKernel failed: %d\\n\", " +
               "status);")
         write("exit(-1);")
         leave
         write("}")
         write(clock + ".count += 1;")

         // Enqueue control data copy (device-to-host).
         // Note that we will use the ready flag to know when this completes.
         write("status = clEnqueueReadBuffer(" + commandQueue + ", " +
               blockControlHandle + ", CL_FALSE, 0, " +
               "sizeof(" + blockControl + "), " +
               "&" + blockControl + ", 1, &taskEvent, NULL);")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clEnqueueReadBuffer failed: %d\\n\", " +
               "status);")
         write("exit(-1);")
         leave
         write("}")

         // Flush.  This forces the command queue to execute.
         write("clFlush(" + commandQueue + ");")

         // Release events.
         write("for(int i = 0; i <= pending; i++) {")
         enter
         write("clReleaseEvent(events[i]);")
         leave
         write("}")
         write("clReleaseEvent(taskEvent);")
         write(blockState + " = OCL_STATE_RUNNING;")

         leave
         write("}")

         write("break;")
         leave
         write("case OCL_STATE_RUNNING:")
         enter

         // Kernel is running or transferring the data block back to the
         // host.  We stay in this state until that finishes.
         // When the data block is transfered, the "ready" flag should
         // be set to 1.
         write("if(" + readyFlag + ") {")
         enter

         // Make sure all the data has been transferred.
         write("clFinish(" + commandQueue + ");")

         // Finish buffer reads for inputs.
         // Note that this is done for both remote and internal edges.
         for (stream <- block.getInputs) {
            val index = stream.destIndex
            val queueName = stream.label + "_data.queue"
            val readval = blockControl + ".input" + index + "_read"
            val offset = stream.label + "_data.offset"
            write("APQ_FinishRead(" + queueName + ", " +
                  readval + " - " + offset + ");")
         }

         write(blockEventCount + " = 0;")
         write(blockState + " = OCL_STATE_FINISHED;")
         leave
         write("} else {")
         enter
         write("break;")
         leave
         write("}")

         // Intentional fall-through to the next state to start
         // device-to-host transfers.
         write("// Fall through.")
         leave
         write("case OCL_STATE_FINISHED:")
         enter

         // Kernel has completed and the data block has been copied back
         // to the host.  Now we need to queue up transfers to move data
         // to the next blocks.
         // We stay in the FINISHED state until all device-to-host transfers
         // have been enqueued.  We then switch to TRANSFERRING until all
         // transfers complete.

         write(blockState + " = OCL_STATE_TRANSFERRING;")

         // Start device-to-host transfers.
         for (stream <- block.getOutputs.filter {
            _.destBlock.device != device
         }) {

            val index = stream.sourceIndex
            val queueName = stream.label + "_data.queue"
            val sentval = blockControl + ".output" + index + "_sent"
            val valueType = stream.valueType
            val streamEvent = stream.label + "_data.event"
            val copied = stream.label + "_data.copied"

            write("if(" + sentval + " > 0) {")
            enter
            write("buf = APQ_StartWrite(" + queueName + ", " + sentval + ");")
            write("if(buf != NULL) {")
            enter
            write("status = clEnqueueReadBuffer(" + commandQueue + ", " +
                  stream.label + "_data.handle, CL_FALSE, 0, " +
                  sentval + " * sizeof(" + valueType + "), " +
                  "buf, 0, NULL, &" +
                  blockEvents + "[" + blockEventCount + "]);")
            write("if(XUNLIKELY(status != CL_SUCCESS)) {")
            enter
            write("fprintf(stderr, \"clEnqueueReadBuffer failed: %d\\n\", " +
                  "status);")
            write("exit(-1);")
            leave
            write("}")
            write(blockEventCount + " += 1;")
            write(copied + " = " + sentval + ";")
            write(sentval + " = 0;")
            leave
            write("} else {")
            enter
            write(blockState + " = OCL_STATE_FINISHED;")
            leave
            write("}")
            leave
            write("}")

         }

         // Check if all reads are enqueued.
         write("if(" + blockState + " == OCL_STATE_TRANSFERRING) {")
         enter

         // Finish writes for streams on the device.
         for (stream <- block.getOutputs.filter {
            _.destBlock.device == device
         }) {
            val queueName = stream.label + "_data.queue"
            val copied = stream.label + "_data.copied"
            val index = stream.sourceIndex
            val sentval = block.label + "_control.output" + index + "_sent"
            write("APQ_FinishWrite(" + queueName + ", " +
                  sentval + " - " + copied + ");")
         }

         // Read the ready flag again so we know when everything is complete.
         write("if(" + blockEventCount + " > 0) {")
         enter
         write(blockControl + ".ap_ready = 0;")
         write("status = clEnqueueReadBuffer(" + commandQueue + ", " +
               blockControlHandle + ", CL_FALSE, 0, " +
               "sizeof(" + blockControl + ".ap_ready), " +
               "&" + blockControl + ".ap_ready, " + blockEventCount + ", " +
               blockEvents + ", NULL);")
         write("if(XUNLIKELY(status != CL_SUCCESS)) {")
         enter
         write("fprintf(stderr, \"clEnqueueReadBuffer failed: %d\\n\", " +
               "status);")
         write("exit(-1);")
         leave
         write("}")
         write("for(int i = 0; i < " + blockEventCount + "; i++) {")
         enter
         write("clReleaseEvent(" + blockEvents + "[i]);")
         leave
         write("}")
         leave
         write("}")

         // Make sure all reads are going.
         write("clFlush(" + commandQueue + ");")

         leave
         write("}")

         write("break;")

         leave
         write("case OCL_STATE_TRANSFERRING:")
         enter

         // Transferring data back to the host.
         // Here we finish writes and switch to OCL_STATE_READY when
         // all transfers are finished.

         write("if(" + blockControl + ".ap_ready) {")
         enter
         write(blockState + " = OCL_STATE_READY;")

         for (stream <- block.getOutputs.filter {
            _.destBlock.device != device
         }) {
            val queueName = stream.label + "_data.queue"
            val copied = stream.label + "_data.copied"
            write("if(" + copied + " > 0) {")
            enter
            write("APQ_FinishWrite(" + queueName + ", " + copied + ");")
            leave
            write("}")
         }

         leave
         write("}")

         write("break;")
         leave
         write("}")
         write("APC_Stop(&" + clock + ");")

      }

      leave
      write("}")
      write("return NULL;")
      leave
      write("}")

   }

   override def emitStats(streams: Traversable[Stream]) {
      for (device <- getDevices(streams)) {
         write("thread_ticks = 0;")
         write("thread_us = 0;")
         write("fprintf(stderr, \"OpenCL Thread " + device.index + ":\\n\");")
         for (block <- getBlocks(device, ap.blocks)) {
            val name = block.blockType.name
            val instance = block.label
            val ticks = instance + "_block.clock.total_ticks"
            val pushes = instance + "_block.clock.count"
            write("ticks = " + ticks + ";")
            write("pushes = " + pushes + ";")
            write("us = (ticks * total_us) / total_ticks;")
            write("fprintf(stderr, \"    " + name + "(" + instance + "): " +
                  "%llu ticks, %llu pushes, %llu us\\n\", ticks, pushes, us);")
            write("thread_ticks += ticks;")
            write("thread_us += us;")
         }
         write("fprintf(stderr, \"    Overall %llu ticks, %llu us " +
               "(%llu%% utilization)\\n\", thread_ticks, thread_us, " +
               "(100 * thread_us) / total_us);")
      }
   }

}

