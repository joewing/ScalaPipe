package examples

import scalapipe._
import scalapipe.dsl._
import scalapipe.kernels._

object LZ77 extends App {

    val compress = false
    val rawFile = "data"
    val compressedFile = "data.cmp"

    val offsetBits = 10
    val lengthBits = 6

    val FileReader = new Kernel {

        val out = output(UNSIGNED16)
        val fn = config(STRING, 'input, rawFile)

        val fd = local(stdio.FILEPTR, 0)

        if (fd == 0) {
            fd = stdio.fopen(fn, "rb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        if (!stdio.feof(fd)) {
            out = stdio.fgetc(fd)
        } else {
            out = 0xFFFF
            stop
        }

    }

    val CompressedFileReader = new Kernel {

        val out = output(UNSIGNED32)
        val fn = config(STRING, 'input, compressedFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "rb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        if (!stdio.feof(fd)) {
            temp  = stdio.fgetc(fd) << 16
            temp |= stdio.fgetc(fd) << 8
            temp |= stdio.fgetc(fd)
            out = temp
        } else {
            out = 0xFFFFFFFF
            stop
        }

    }

    val FileWriter = new Kernel {

        val in = input(UNSIGNED16)
        val fn = config(STRING, 'output, rawFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "wb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        temp = in
        if (temp == 0xFFFF) {
            stdio.fclose(fd)
            stdio.exit(0)
        } else {
            stdio.fputc(temp, fd)
        }

    }

    val CompressedFileWriter = new Kernel {

        val in = input(UNSIGNED32)
        val fn = config(STRING, 'output, compressedFile)

        val fd = local(stdio.FILEPTR, 0)
        val temp = local(UNSIGNED32)

        if (fd == 0) {
            fd = stdio.fopen(fn, "wb")
            if (fd == 0) {
                stdio.printf("ERROR: could not open %s\n", fn)
                stdio.exit(-1)
            }
        }

        temp = in
        if (temp == 0xFFFFFFFF) {
            stdio.fclose(fd)
            stdio.exit(0)
        } else {
            stdio.fputc((temp >> 16) & 0xFF, fd)
            stdio.fputc((temp >>  8) & 0xFF, fd)
            stdio.fputc((temp >>  0) & 0xFF, fd)
        }

    }

    val Compress = new LZ77Compress(offsetBits, lengthBits)

    val Decompress = new LZ77Decompress(offsetBits, lengthBits)

    val app = new Application {
        if (compress) {
            val input = FileReader()
            val compressed = Compress(input)
            CompressedFileWriter(compressed)
        } else {
            val input = CompressedFileReader()
            val raw = Decompress(input)
            FileWriter(raw)
        }
    }
    app.emit("lz77")

}

