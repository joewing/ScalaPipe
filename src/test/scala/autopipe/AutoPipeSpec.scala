package autopipe

import org.scalatest._
import autopipe.dsl.AutoPipeBlock

class AutoPipeSpec extends UnitSpec {

    def createAutoPipe(): AutoPipe = {
        val ap = new AutoPipe()
        ap.createBlock(new AutoPipeBlock("TestBlock"))
        ap
    }

    def createBlock(ap: AutoPipe) = {
        new Block(ap, "TestBlock")
    }

    def createApplication(ap: AutoPipe,
                          edge1: Edge = null,
                          edge2: Edge = null): Seq[Block] = {
        val block1 = createBlock(ap)
        val block2 = createBlock(ap)
        val block3 = createBlock(ap)
        val block4 = createBlock(ap)
        val sl1 = block1()
        val stream1 = sl1(0)
        val stream2 = sl1(1)
        if (edge1 != null) {
            stream1.setEdge(edge1)
        }
        if(edge2 != null) {
            stream2.setEdge(edge2)
        }
        val sl2 = block2((null, stream1))
        val sl3 = block3((null, stream2))
        block4((null, sl2(0)), (null, sl3(0)))
        Seq(block1, block2, block3, block4)
    }

    "getDevice" should "return a CPU device by default" in {
        val ap = createAutoPipe()
        val blocks = createApplication(ap)
        val getDevice = PrivateMethod[Device]('getDevice)
        val device = ap invokePrivate getDevice(blocks)
        assert(device.deviceType.platform == Platforms.C)
    }

    "getDevice" should "return the correct device type" in {
        val ap = createAutoPipe()
        val blocks = createApplication(ap, CPU2FPGA(), CPU2FPGA())
        val getDevice = PrivateMethod[Device]('getDevice)
        val device = ap invokePrivate getDevice(blocks)
        assert(device.deviceType.platform == Platforms.HDL)
    }

    "getDevice" should "raise an error on an invalid mapping" in {
        val ap = createAutoPipe()
        val blocks = createApplication(ap, CPU2FPGA(), CPU2CPU())
        val getDevice = PrivateMethod[Device]('getDevice)
        intercept[RuntimeException] {
            ap invokePrivate getDevice(blocks)
        }
    }

    "getConnectedBlocks" should "return connected blocks" in {
        val ap = createAutoPipe()
        val blocks = createApplication(ap)
        val getConnectedBlocks = PrivateMethod[Seq[Block]]('getConnectedBlocks)
        val lst = ap invokePrivate getConnectedBlocks(blocks(2))
        assert(blocks.length == lst.length)
    }

}
