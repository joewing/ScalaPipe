package autopipe.opt

import autopipe._
import autopipe.dsl._

class DSESpec extends PassTestSpec {

    "DSE" should "eliminate extra states" in {

        val block = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val y = state(SIGNED32)
            val z = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.assign, x, literal(1))
    
            label(2)
            op(NodeType.assign, x, in)
    
            label(3)
            op(NodeType.add, y, x, literal(1))
    
            label(4)
            op(NodeType.add, z, x, literal(1))
    
            label(5)
            op(NodeType.add, z, z, y)
    
            label(6)
            op(NodeType.assign, out, z)
    
        }
    
        val expected = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val y = state(SIGNED32)
            val z = state(SIGNED32)
    
            label(0)
            start(2)
    
            label(2)
            op(NodeType.assign, x, in)
    
            label(3)
            op(NodeType.add, y, x, literal(1))
    
            label(4)
            op(NodeType.add, z, x, literal(1))
    
            label(5)
            op(NodeType.add, z, z, y)
    
            label(6)
            op(NodeType.assign, out, z)
    
        }

        checkPass(block, expected, DSE)

    }

}
