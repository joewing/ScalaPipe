package scalapipe.opt

import scalapipe._
import scalapipe.dsl._

class CombineVariablesSpec extends PassTestSpec {

    "CombineVariables" should "combine variables" in {

        val block = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val y = state(SIGNED32)
            val z = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.assign, x, in)
    
            label(2)
            op(NodeType.add, y, x, literal(1))
    
            label(3)
            op(NodeType.assign, x, y)
    
            label(4)
            op(NodeType.assign, z, literal(1))
    
            label(5)
            op(NodeType.add, x, x, z)
    
            label(6)
            op(NodeType.assign, out, x)
    
        }

        val expected = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val y = state(SIGNED32)
            val z = state(SIGNED32)
    
            label(0)
            start
    
            label(1)
            op(NodeType.assign, x, in)
    
            label(2)
            op(NodeType.add, z, x, literal(1))
    
            label(3)
            op(NodeType.assign, x, z)
    
            label(4)
            op(NodeType.assign, z, literal(1))
    
            label(5)
            op(NodeType.add, x, x, z)
    
            label(6)
            op(NodeType.assign, out, x)
    
        }

        checkPass(block, expected, CombineVariables)

    }
}
