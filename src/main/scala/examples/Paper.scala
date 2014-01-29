package examples

import scalapipe.kernels.ANY_KERNEL
import scalapipe.kernels.stdio
import scalapipe._
import scalapipe.dsl._

object Paper {

    def main(args: Array[String]) {

        val levels = 0
        val width = 100
        val height = 100
        val iterations = 256 / (1 << levels)
        val randSeed = 15

        val LFSR = new Kernel {

            val out = output(UNSIGNED32)
            val state = local(UNSIGNED32, 1)

            // Taps at 32, 31, 29, 1.
            state = (state >> 1) ^ (-(state & 1) & 0xD0000001);

            // Output the result.
            out = state

        }

        val MT19937 = new Kernel {

            val out = output(UNSIGNED32)

            val mt = local(new AutoPipeArray(UNSIGNED32, 624))
            val index = local(UNSIGNED32, 0)
            val configured = local(BOOL, false)
            val i = local(UNSIGNED32, randSeed)
            val j = local(UNSIGNED32)
            val y = local(UNSIGNED32)

            if (configured) {
                if (index == 624) {
                    i = 0
                    while (i < 624) {
                        j = i + 1
                        if (j == 624) {
                            j = 0
                        }
                        y = mt(i) >> 31
                        y += mt(j) & 0x7FFFFFFF
                        j = i + 397
                        if (j > 623) {
                            j -= 624
                        }
                        mt(i) = mt(j) ^ (y >> 1)
                        if (y & 1) {
                            mt(i) ^= 0x9908b0df
                        }
                        i += 1
                    }
                    index = 0
                }
                y = mt(index)
                y ^= (y >> 11)
                y ^= ((y << 7) & 0x9d2c5680)
                y ^= ((y << 15) & 0xefc60000)
                y ^= (y >> 18)
                index += 1
                out = y
            } else {
                mt(index) = i
                i = 0x6c078965 * (i ^ (i >> 30))
                i += index
                index += 1
                configured = index == 624;
            }

        }

        val Random = LFSR

        class Split(t: AutoPipeType) extends Kernel {

            val in    = input(t)
            val out0 = output(t)
            val out1 = output(t)

            if (avail(out0)) {
                out0 = in
            }
            if (avail(out1)) {
                out1 = in
            }

        }

        class Average(t: AutoPipeType) extends Kernel {

            val in0 = input(t)
            val in1 = input(t)
            val out = output(t)

            out = (in0 + in1) / 2

        }

        val Walk = new Kernel {

            val x0 = input(UNSIGNED32)
            val y0 = output(UNSIGNED32)

            val xstart = local(SIGNED32, 0)
            val ystart = local(SIGNED32, 0)
            val xcoord = local(SIGNED32, 0)
            val ycoord = local(SIGNED32, 0)
            val iter = local(UNSIGNED32, 0)
            val total = local(UNSIGNED64, 0)

            val random = local(UNSIGNED32)
            val dir = local(UNSIGNED32)
            val i = local(UNSIGNED32)
            val temp = local(UNSIGNED32)
            val atBoundary = local(UNSIGNED8)

            random = x0
            i = 0
            while (i < 16) {

                temp = 0
                dir = random & 3
                switch(dir) {
                    when(0) {
                        xcoord += 1
                        atBoundary = xcoord >= width
                    }
                    when(1) {
                        xcoord -= 1
                        temp = 100
                        atBoundary = xcoord < 0
                    }
                    when(2) {
                        ycoord += 1
                        atBoundary = ycoord >= height
                    }
                    when(3) {
                        ycoord -= 1
                        atBoundary = ycoord < 0
                    }
                }

                if (atBoundary) {
                    total += temp
                    iter += 1
                    if (iter == iterations) {
                        y0 = total / iterations
                        total = 0
                        iter = 0
                        xstart += 1
                        if (xstart == width) {
                            xstart = 0
                            ystart += 1
                        }
                    }
                    xcoord = xstart
                    ycoord = ystart
                }

                random >>= 2
                i += 1
            }

        }

        val Print = new Kernel {

            val x0 = input(UNSIGNED32)

            val filename = config(STRING, 'filename, "results.txt")

            val fd = local(stdio.FILEPTR, 0)
            val x = local(UNSIGNED32, 0)
            val y = local(UNSIGNED32, 0)

            if (fd == 0) {
                fd = stdio.fopen(filename, "w")
                if (fd == 0) {
                    stdio.printf("""Could not open %s\n""", filename)
                }
            }

            stdio.fprintf(fd, """%u """, x0)
            x += 1
            if (x == width) {
                stdio.fprintf(fd, """\n""")
                stdio.fflush(fd)
                x = 0
                y += 1
                if (y == height) {
                    stdio.exit(0)
                }
            }

        }

        val SplitU32 = new Split(UNSIGNED32)
        val AverageU32 = new Average(UNSIGNED32)

        trait SplitJoin extends AutoPipeApp {
            def splitJoin(input:    Stream,
                          levels:   Int,
                          split:    Kernel,
                          join:     Kernel)
                         (f: Stream => Stream): Stream = {

                val ins = input.iteratedMap(levels, split)
                val outs = Array.tabulate(1 << levels) {
                    x => f(ins(x))
                }
                iteratedFold(outs, join)

            }
        }

        val laplace = new AutoPipeApp with SplitJoin {

            val random = Random()
            val result = splitJoin(random, levels, SplitU32, AverageU32) {
                Walk(_)
            }
            Print(result)

            map(ANY_KERNEL -> Print, FPGA2CPU())

        }
        laplace.emit("paper")

    }

}

