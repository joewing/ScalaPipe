package autopipe.opt

import autopipe._
import autopipe.dsl._

class ReassignStatesSpec extends PassTestSpec {

    "ReassignStates" should "reassign states" in {

        val block = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
    
            label(0)
            start(5)
    
            label(5)
            op(NodeType.assign, x, in)
    
            label(8)
            op(NodeType.add, x, x, literal(1))
    
            label(9)
            op(NodeType.assign, out, x)
    
        }
    
        val expected = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.assign, x, in)
    
            label(2)
            op(NodeType.add, x, x, literal(1))
    
            label(3)
            op(NodeType.assign, out, x)
    
        }

        checkPass(block, expected, ReassignStates)

    }

}
