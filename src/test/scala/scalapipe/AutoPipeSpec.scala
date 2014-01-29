package scalapipe

import org.scalatest._
import scalapipe.dsl._

class AutoPipeSpec extends UnitSpec {

    val apb = new Kernel("TestKernel")

    def createAutoPipe(): AutoPipe = {
        val ap = new AutoPipe()
        ap.createInstance(apb)
        ap
    }

    def createInstance(ap: AutoPipe) = {
        new KernelInstance(ap, apb)
    }

    def createApplication(ap: AutoPipe,
                          edge1: Edge = null,
                          edge2: Edge = null): Seq[KernelInstance] = {
        val kernel1 = createInstance(ap)
        val kernel2 = createInstance(ap)
        val kernel3 = createInstance(ap)
        val kernel4 = createInstance(ap)
        val sl1 = kernel1()
        val stream1 = sl1(0)
        val stream2 = sl1(1)
        if (edge1 != null) {
            stream1.setEdge(edge1)
        }
        if(edge2 != null) {
            stream2.setEdge(edge2)
        }
        val sl2 = kernel2((null, stream1))
        val sl3 = kernel3((null, stream2))
        kernel4((null, sl2(0)), (null, sl3(0)))
        Seq(kernel1, kernel2, kernel3, kernel4)
    }

    "getDevice" should "return a CPU device by default" in {
        val ap = createAutoPipe()
        val kernels = createApplication(ap)
        val getDevice = PrivateMethod[Device]('getDevice)
        val device = ap invokePrivate getDevice(kernels)
        assert(device.deviceType.platform == Platforms.C)
    }

    "getDevice" should "return the correct device type" in {
        val ap = createAutoPipe()
        val kernels = createApplication(ap, CPU2FPGA(), CPU2FPGA())
        val getDevice = PrivateMethod[Device]('getDevice)
        val device = ap invokePrivate getDevice(kernels)
        assert(device.deviceType.platform == Platforms.HDL)
    }

    "getDevice" should "raise an error on an invalid mapping" in {
        val ap = createAutoPipe()
        val kernels = createApplication(ap, CPU2FPGA(), CPU2CPU())
        val getDevice = PrivateMethod[Device]('getDevice)
        val oldCount = Error.errorCount
        ap invokePrivate getDevice(kernels)
        assert(Error.errorCount == oldCount + 1)
    }

    "getConnectedKernels" should "return connected kernels" in {
        val ap = createAutoPipe()
        val kernels = createApplication(ap)
        val getConnectedKernels =
            PrivateMethod[Seq[KernelInstance]]('getConnectedKernels)
        val lst = ap invokePrivate getConnectedKernels(kernels(2))
        assert(kernels.length == lst.length)
    }

}
