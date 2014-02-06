package examples

import scalapipe.dsl._
import scalapipe.kernels._

object Mandelbrot {

    private def usage: Nothing = {
        println("Mandelbrot [options]")
        println("options:")
        println("  -w <int>      Width")
        println("  -h <int>      Height")
        println("  -l <int>      Levels of split blocks")
        println("  -i <int>      Number of iterations")
        println("  -hw           Use hardware")
        sys.exit(-1)
    }

    type OptionMap = Map[Symbol, Int]

    private def parse(args: List[String], values: OptionMap): OptionMap = {
        args match {
            case "-w"  :: v :: t => parse(t, values + (('width, v.toInt)))
            case "-h"  :: v :: t => parse(t, values + (('height, v.toInt)))
            case "-l"  :: v :: t => parse(t, values + (('levels, v.toInt)))
            case "-i"  :: v :: t => parse(t, values + (('iterations, v.toInt)))
            case "-hw" :: t      => parse(t, values + (('hw, 1)))
            case Nil => values
            case other => println("invalid option: " + other); usage
        }
    }

    def main(args: Array[String]) {

        // Set up defaults.
        val defaults = Map('width -> 64)        ++
                       Map('height -> 64)       ++
                       Map('iterations -> 16)   ++
                       Map('levels -> 0)        ++
                       Map('hw -> 1)

        // Parse arguments.
        val options = parse(args.toList, defaults)
        val width = options('width)
        val height = options('height)
        val maxIterations = options('iterations)
        val levels = options('levels)
        val use_hw = options('hw) == 1

        // Display what we will be generating.
        println("Generating mandelbrot application:")
        println("  Width:      " + width)
        println("  Height:     " + height)
        println("  Iterations: " + maxIterations)
        println("  Levels:     " + levels)
        println("  Platform:   " + (if (use_hw) "hardware" else "software"))

        val FIXED = new Fixed(32, 16)

        val Start = new Kernel {
            val out = output(UNSIGNED8)
            out = 1
            stop
        }

        val PixelGenerator = new Kernel {

            val in = input(UNSIGNED8)
            val pixel = output(UNSIGNED32)

            val i = local(UNSIGNED32, 0)
            val j = local(UNSIGNED32, 0)
            val running = local(UNSIGNED8, 0)

            if (running) {
                pixel = (i << 16) | j
                i += 1
                if (i == width) {
                    i = 0
                    j += 1
                    if (j == height) {
                        stop
                    }
                }
            } else {
                running = in
            }

        }

        val Iterate = new Kernel {

            val in = input(UNSIGNED32)
            val out = output(UNSIGNED64)

            val pixel = local(UNSIGNED64)
            val iteration = local(UNSIGNED32)
            val x0 = local(FIXED)
            val y0 = local(FIXED)
            val x = local(FIXED)
            val y = local(FIXED)
            val temp = local(FIXED)

            pixel = in
            x0 = (3.5 * cast(pixel >> 16, FIXED)) / width - 2.5
            y0 = (2.0 * cast(pixel & 0xFFFF, FIXED)) / height - 1.0

            x = 0
            y = 0
            iteration = 0
            while (x * x + y * y < 4 && iteration < maxIterations) {
                temp = x * x - y * y + x0
                y = 2 * x * y + y0
                x = temp
                iteration += 1
            }
            out = (pixel << 16) | iteration

        }

        val Print = new Kernel {

            val result = input(UNSIGNED64)

            val BUFFER_TYPE = new Vector(UNSIGNED32, width * height)

            val temp = local(UNSIGNED64)
            val count = local(UNSIGNED32, 0)
            val fd = local(stdio.FILEPTR, 0)
            val buffer = local(BUFFER_TYPE)
            val x = local(UNSIGNED32)
            val y = local(UNSIGNED32)

            if (fd == 0) {
                fd = stdio.fopen("results.txt", "w")
                if (fd == 0) {
                    stdio.exit(-1)
                }
            }

            temp = result
            x = (temp >> 32) & 0xFFFF
            y = (temp >> 16) & 0xFFFF
            buffer(y * width + x) = temp & 0xFFFF

            count += 1
            if (count == width * height) {

                y = 0
                while (y < height) {
                    x = 0
                    while (x < width) {
                        stdio.fprintf(fd, "%u ", buffer(y * width + x))
                        x += 1
                    }
                    stdio.fprintf(fd, "\n")
                    y += 1
                }

                stdio.fclose(fd)
                stdio.exit(0)
            }

        }

        val SplitPixels = new Split(UNSIGNED32)
        val JoinResults = new Join(UNSIGNED64)

        val Mandelbrot = new Application {

            val pixels = PixelGenerator(Start())
            val splits = pixels.iteratedMap(levels, SplitPixels)

            val results = Array.tabulate(1 << levels)(i => {
                Iterate(splits(i))(0)
            })
            val result = iteratedFold(results, JoinResults)

            Print(result)

            if (use_hw) {
                map(Start -> ANY_KERNEL, CPU2FPGA())
                map(ANY_KERNEL -> Print, FPGA2CPU())
            }

        }
        Mandelbrot.emit("mandelbrot")

    }

}
