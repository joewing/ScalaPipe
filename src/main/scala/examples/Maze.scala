package examples

import scalapipe.kernels._

import scalapipe._
import scalapipe.dsl._

object Maze {

    // Show usage (-h option).
    private def usage: Nothing = {
        println("Maze [options]")
        println("options:")
        println("  -w <int>      Width (must be a multiple of 2)")
        println("  -h <int>      Height (must be a multiple of 2)")
        println("  -s <int>      Random number seed")
        println("  -hw             Use hardware")
        sys.exit(-1)
    }

    type OptionMap = Map[Symbol, Int]

    // Parse arguments.
    private def parse(args: List[String], values: OptionMap): OptionMap = {
        args match {
            case "-w"  :: v :: t => parse(t, values + (('width, v.toInt)))
            case "-h"  :: v :: t => parse(t, values + (('height, v.toInt)))
            case "-s"  :: v :: t => parse(t, values + (('seed, v.toInt)))
            case "-hw" :: t      => parse(t, values + (('hw, 1)))
            case Nil             => values
            case other => println("invalid option: " + other); usage
        }
    }

    def main(args: Array[String]) {

        // Set up defaults.
        val defaults = Map('width -> 25)    ++
                       Map('height -> 25)   ++
                       Map('seed -> 0)      ++
                       Map('hw -> 1)

        // Parse arguments.
        val options = parse(args.toList, defaults)
        val width = (options('width) + 1) & ~1
        val height = (options('height) + 1) & ~1
        val seed = options('seed)
        val use_hw = options('hw) == 1

        // Display what we will be generating.
        println("Generating maze application:")
        println("  Width:       " + width)
        println("  Height:      " + height)
        println("  Seed:        " + seed)
        println("  Platform:    " + (if (use_hw) "hardware" else "software"))

        val GenRun = new Kernel {

            val rand = input(UNSIGNED32)
            val runLength = output(UNSIGNED32)
            val north = output(UNSIGNED32)

            val i = local(UNSIGNED32)
            val temp = local(UNSIGNED32)

            // Generate a run.
            // We generate exponentially distributed values to make the
            // maze more interesting.
            i = 1
            temp = rand
            while ((temp & 1) == 1) {
                i += 1
                temp >>= 1
            }
            runLength = i

            // Generate a uniform random number for the location of the
            // northward passage.  This value must not be larger than the
            // length of the run.
            temp = i
            while (temp >= i) {
                temp = rand % 16
            }
            north = temp

        }

        val CarveMaze = new Kernel {

            val runInput = input(UNSIGNED32)
            val northInput = input(UNSIGNED32)
            val maze = output(UNSIGNED8)

            val ROWTYPE = Vector(UNSIGNED8, width)

            val next = local(ROWTYPE)
            val x = local(UNSIGNED32, 0)
            val i = local(UNSIGNED32)
            val run = local(UNSIGNED32)
            val north = local(UNSIGNED32)

            // Get the length of the run and location of northward passage.
            // Note that the length of the run must be smaller than the
            // remaining space on this row, so we drop runs until we get one
            // that works.
            run = width
            while (run > (int(width) - x) / 2) {
                run = runInput
                north = northInput
            }

            // Cut out the passage.
            // We output the nortward passages and buffer up the
            // eastward passages since they go underneath.
            i = 0
            while (i < run) {

                // Northward passage.
                maze = i != north
                maze = 1

                // Eastward passage.
                next(x) = 0
                x += 1
                next(x) = i + 1 >= run
                x += 1

                i += 1

            }

            // If we're at the end of the row, output the eastward
            // passages and reset.
            if (x >= width) {

                // Output the eastward passages.
                i = 0
                while (i < width) {
                    maze = next(i)
                    i += 1
                }

                // Prepare for the next row.
                x = 0

            }

        }

        val Print = new Kernel {

            val maze = input(UNSIGNED8)

            val x = local(UNSIGNED32, 0)
            val y = local(UNSIGNED32, 0)
            val i = local(UNSIGNED32, 0)

            if (x == 0) {

                // Write the top two rows.
                if (y == 0) {
                    stdio.printf("1 0 ")
                    i = 1
                    while (i < width) {
                        stdio.printf("1 ")
                        i += 1
                    }
                    stdio.printf("\n1 ")
                    i = 1
                    while (i < width) {
                        stdio.printf("0 ")
                        i += 1
                    }
                    stdio.printf("1\n")
                }

                // Write the block at the beginning of each row.
                stdio.printf("1 ")
            }

            // Output the body of the maze.
            if (maze) {
                stdio.printf("1 ")
            } else {
                stdio.printf("0 ")
            }

            x += 1
            if (x >= width) {

                // New line at the end of each row.
                stdio.printf("\n")
                x = 0
                y += 1

                if (y >= height) {
                    // Output the bottom row and exit.
                    i = 1
                    while (i < width) {
                        stdio.printf("1 ")
                        i += 1
                    }
                    stdio.printf("0 1\n")
                    stdio.exit(0)
                }

            }

        }

        val Maze = new Application {

            param('trace, true)
            val random = MT19937(GenState('seed -> seed))
            val runs = GenRun(random)
            val maze = CarveMaze(runs(0), runs(1))
            Print(maze)

            if (use_hw) {
                map(GenState -> MT19937, CPU2FPGA())
                map(ANY_KERNEL -> Print, FPGA2CPU())
            }

//map(MT19937 -> ANY_KERNEL, CPU2GPU())
//map(ANY_KERNEL -> GenRun, CPU2GPU())
//map(ANY_KERNEL -> CarveMaze, GPU2CPU())
//map(ANY_KERNEL -> CarveMaze, CPU2GPU())
//map(ANY_KERNEL -> Print, GPU2CPU())

        }
        Maze.emit("maze")

    }

}
