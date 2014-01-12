package autopipe.opt

import autopipe._
import autopipe.dsl._

class StrengthReductionSpec extends PassTestSpec {

    "StrengthReduction" should "replace expensive operations" in {

        val block = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.add, x, x, x)
    
            label(2)
            op(NodeType.sub, x, x, x)
    
            label(3)
            op(NodeType.mul, x, x, literal(1))
    
        }
    
        val expected = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.shl, x, x, literal(1))
    
            label(2)
            op(NodeType.assign, x, literal(0))
    
            label(3)
            op(NodeType.assign, x, x)
    
        }

        checkPass(block, expected, StrengthReduction)

    }

}
