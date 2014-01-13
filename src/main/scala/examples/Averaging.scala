
package examples

import blocks.stdio
import blocks.ANY_BLOCK

import autopipe._
import autopipe.dsl._

object Averaging {

    def main(args: Array[String]) {

        // Declare the "Random" block.
        val Random = new AutoPipeBlock {

            // This block has a single output and a configuration option.
            val y0 = output(UNSIGNED32)
            val iterations = config(UNSIGNED64, 'iterations, 0)

            // The block code.  Since this block has no inputs, this code will
            // run every time the "go" function is executed.  Each write to
            // an output port does a send.  Here we generate a random number
            // in the range [0, 8).  If iterations is set, we only send
            // that number of random numbers.
            y0 = stdio.rand() % 8
            if (iterations > 0) {
                iterations -= 1
                if (iterations == 0) {
                    stop
                }
            }

        }

        // Create a polymorphic "Add" block.
        class AddBlock(t: AutoPipeType) extends AutoPipeFunction {

            // This block takes two inputs and has one output.
            val x0 = input(t)
            val x1 = input(t)
            returns(t)

            // Since there are inputs, this will go in the "push" call.
            // The generated code will wait until all necessary ports are
            // available, and then write the output.
            ret(x1 + x0)

        }

        // Declare the "Add" block for UNSIGNED32.
        val Add = new AddBlock(UNSIGNED32)

        // Create a polymorphic "Half" block.
        class HalfBlock(t: AutoPipeType) extends AutoPipeBlock {

            // This block has a single input and a single output.
            val x0 = input(t)
            val y0 = output(t)

            // Read a single input, divide it by two, and write the output.
            y0 = x0 / 2
        }

        // Declare the "Half" block for UNSIGNED32.
        val Half = new HalfBlock(UNSIGNED32)

        // Declare the "Print" block.
        val Print = new AutoPipeBlock {

            // This block has only an input.
            val x0 = input(UNSIGNED32)

            // Read a value from the input port and print it.
            stdio.printf("""%u\n""", x0)

        }

        // Define the application.
        // Since there are blocks that are only declared in software and
        // no "edge" blocks are used, we know that all blocks must use the
        // software implementation.
        val app = new AutoPipeApp {

            val iterations = 1000
            val random0 = Random('iterations -> iterations)
            val random1 = Random('iterations -> iterations)
            val result = Half(Add(random0, random1))
            Print(result)

        }

        // Emit the application code.
        app.emit("averaging")

    }

}

