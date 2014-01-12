package autopipe.opt

import autopipe._
import autopipe.dsl._

class DCESpec extends PassTestSpec {

    "DCE" should "eliminate dead code" in {

        val block = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val t = state(BOOL)
    
            label(0)
            start
    
            label(1)
            goto(3)
    
            label(2)
            op(NodeType.assign, x, in)
    
            label(3)
            op(NodeType.add, x, x, literal(1))
    
            label(4)
            op(NodeType.assign, out, x)
    
            label(5)
            op(NodeType.sub, x, x, literal(1))
    
            label(6)
            op(NodeType.le, t, x, literal(0))
    
            label(7)
            cond(t, 9, 4)
    
            label(8)
            op(NodeType.assign, x, literal(0))
    
            label(9)
            stop
    
        }
    
        val expected = new IRBuilder {
    
            val in = input(SIGNED32)
            val out = output(SIGNED32)
    
            val x = state(SIGNED32)
            val t = state(BOOL)
    
            label(0)
            start
    
            label(1)
            goto(3)
    
            label(3)
            op(NodeType.add, x, x, literal(1))
    
            label(4)
            op(NodeType.assign, out, x)
    
            label(5)
            op(NodeType.sub, x, x, literal(1))
    
            label(6)
            op(NodeType.le, t, x, literal(0))
    
            label(7)
            cond(t, 9, 4)
    
            label(9)
            stop
    
        }

        checkPass(block, expected, DCE)

    }

}
