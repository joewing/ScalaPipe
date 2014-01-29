package examples

import scalapipe.kernels._
import scalapipe._
import scalapipe.dsl._

object Search {

    // Number of search terms to support.
    val maxTerms = 2

    // Set to use hardware for searching.
    val use_hw = false

    def main(args: Array[String]) {

        val TERM = new AutoPipeArray(UNSIGNED64, 2)

        // Block to read a list of terms of length up to 8.
        val TermReader = new Kernel {

            val y0 = output(TERM)
            val filename = config(STRING, 'filename, "../terms.txt")

            val fp = local(stdio.FILEPTR, 0)
            val index = local(UNSIGNED32, 0)
            val done = local(UNSIGNED8)
            val ch = local(UNSIGNED32)
            val term_buffer = local(UNSIGNED64)
            val mask_buffer = local(UNSIGNED64)
            val buffer = local(TERM)

            if (fp == 0) {
                fp = stdio.fopen(filename, "r")
                if (fp == 0) {
                    stdio.printf("""Could not open term file: %s\n""", filename)
                    stdio.exit(0)
                }
            } else {
                if (index < maxTerms) {
                    buffer(0) = 0
                    buffer(1) = 0
                    done = 0
                    while (!done) {
                        ch = stdio.fgetc(fp)
                        if (ch <> '\n' && ch < 255) {
                            buffer(0) = (buffer(0) << 8) | ch
                            buffer(1) = (buffer(1) << 8) | 255
                        } else {
                            done = 1
                        }
                    }
                    y0 = buffer
                    index = index + 1
                } else {
                    stdio.fclose(fp)
                    stop
                }
            }

        }

        // Block to read data.
        val FileReader = new Kernel {

            val y0 = output(UNSIGNED8)
            val filename = config(STRING, 'filename, "../data.txt")

            val fp = local(stdio.FILEPTR, 0)

            if (fp == 0) {
                fp = stdio.fopen(filename, "r")
                if (fp == 0) {
                    stdio.printf("""Could not open data file: %s\n""", filename)
                    stdio.exit(0)
                }
            } else {
                y0 = stdio.fgetc(fp)
                if (stdio.feof(fp)) {
                    stdio.fclose(fp)
                    stop
                }
            }

        }

        // Block to distribute terms among the search engines.
        // Note that "maxTerms" determines how many ways to split.
        val SplitTerms = new Kernel {

            val x0 = input(TERM)
            val outs = Array.tabulate(maxTerms) { i => output(TERM) }

            val index = local(UNSIGNED32, 0)
            val temp = local(TERM)

            temp = x0
            for (o <- outs.zipWithIndex) {
                val output = o._1
                val index = o._2
                if (index == index) {
                    output = temp
                }
            }
            if (index + 1 == maxTerms) {
                index = 0
            } else {
                index = index + 1
            }

        }

        // Block to distribute data among the search engines.
        val SplitData = new Kernel {
            val x0 = input(UNSIGNED8)
            val outs = Array.tabulate(maxTerms) { i => output(UNSIGNED8) }
            val temp = local(UNSIGNED8)
            temp = x0
            for (o <- outs) {
                o = temp
            }
        }

        // Block to do the searching.
        val SearchData = new Kernel {
            val conf = input(TERM)
            val data = input(UNSIGNED8)
            val hits = output(UNSIGNED64)

            val term = local(TERM)
            val offset = local(UNSIGNED64, 0)
            val last = local(UNSIGNED64, 0)
            val configured = local(UNSIGNED8, 0)
            val temp = local(UNSIGNED64)

            if (configured == 0) {
                term = conf
                configured = 1
            } else {
                last = (last << 8) | data
                offset = offset + 1
                if ((last & term(1)) == term(0)) {
                    hits = offset
                }
            }

        }

        // Block to combine results.
        val Combine = new Kernel {

            val ins = Array.tabulate(maxTerms) { i => input(UNSIGNED64) }
            val y0 = output(UNSIGNED64)

            for (i <- ins) {
                if (avail (i)) {
                    y0 = i
                }
            }

        }

        // Block to print results.
        val Print = new Kernel {
            val x0 = input(UNSIGNED64)

            stdio.printf("""Match at offset %lu\n""", x0)
        }

        val Search = new AutoPipeApp {

            val terms = TermReader()
            val splitTerms = SplitTerms(terms)
            val data = FileReader()
            val splitData = SplitData(data)
            val hits = Array.tabulate(maxTerms) { i =>
                val term = splitTerms(i)
                val data = splitData(i)
                SearchData(term, data)(0)
            }
            val results = Combine(hits)
            Print(results)

            if (use_hw) {
                map(terms, CPU2FPGA())
                map(data, CPU2FPGA())
                map(results, FPGA2CPU())
            }

        }

        Search.emit("search")

    }

}
